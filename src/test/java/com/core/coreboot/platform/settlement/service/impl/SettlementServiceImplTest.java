package com.core.coreboot.platform.settlement.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.core.coreboot.exception.CustomException;
import com.core.coreboot.exception.ExceptionEnum;
import com.core.coreboot.platform.audit.entity.AuditLog;
import com.core.coreboot.platform.audit.mapper.AuditLogMapper;
import com.core.coreboot.platform.common.enums.ConsumptionOrderStatus;
import com.core.coreboot.platform.common.enums.RoleCode;
import com.core.coreboot.platform.common.enums.SettlementItemType;
import com.core.coreboot.platform.common.enums.SettlementPeriodStatus;
import com.core.coreboot.platform.common.enums.SettlementStatus;
import com.core.coreboot.platform.common.enums.StoreSettlementStatus;
import com.core.coreboot.platform.common.enums.SysUserStatus;
import com.core.coreboot.platform.consumption.entity.ConsumptionOrder;
import com.core.coreboot.platform.consumption.mapper.ConsumptionOrderMapper;
import com.core.coreboot.platform.merchant.entity.Store;
import com.core.coreboot.platform.merchant.mapper.StoreMapper;
import com.core.coreboot.platform.settlement.entity.SettlementPeriod;
import com.core.coreboot.platform.settlement.entity.StoreSettlement;
import com.core.coreboot.platform.settlement.entity.StoreSettlementItem;
import com.core.coreboot.platform.settlement.mapper.SettlementPeriodMapper;
import com.core.coreboot.platform.settlement.mapper.StoreSettlementItemMapper;
import com.core.coreboot.platform.settlement.mapper.StoreSettlementMapper;
import com.core.coreboot.platform.settlement.model.SettlementConfirmCommand;
import com.core.coreboot.platform.settlement.model.SettlementGenerateCommand;
import com.core.coreboot.platform.settlement.model.SettlementPaymentCommand;
import com.core.coreboot.platform.staff.entity.SysUser;
import com.core.coreboot.platform.staff.mapper.StaffAuthorityMapper;
import com.core.coreboot.platform.staff.mapper.SysUserMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SettlementServiceImplTest {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 8, 18, 0);

    @Mock
    private SettlementPeriodMapper settlementPeriodMapper;
    @Mock
    private StoreSettlementMapper storeSettlementMapper;
    @Mock
    private StoreSettlementItemMapper storeSettlementItemMapper;
    @Mock
    private ConsumptionOrderMapper consumptionOrderMapper;
    @Mock
    private StoreMapper storeMapper;
    @Mock
    private SysUserMapper sysUserMapper;
    @Mock
    private StaffAuthorityMapper staffAuthorityMapper;
    @Mock
    private AuditLogMapper auditLogMapper;

    private SettlementServiceImpl service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-09-08T18:00:00Z"), ZoneOffset.UTC);
        service = new SettlementServiceImpl(
                settlementPeriodMapper,
                storeSettlementMapper,
                storeSettlementItemMapper,
                consumptionOrderMapper,
                storeMapper,
                sysUserMapper,
                staffAuthorityMapper,
                auditLogMapper,
                new ObjectMapper().findAndRegisterModules(),
                clock
        );
    }

    @Test
    void shouldGenerateClosedMonthFromImmutableConsumptionSnapshots() {
        allowSuperAdmin();
        SettlementPeriod period = openPeriod();
        Store store = store();
        ConsumptionOrder order = completedOrder();
        AtomicReference<StoreSettlement> savedSettlement = new AtomicReference<>();

        when(settlementPeriodMapper.insertOpenPeriod(
                "2026-08",
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 31)
        )).thenReturn(1);
        when(settlementPeriodMapper.selectByPeriodCodeForUpdate("2026-08")).thenReturn(period);
        when(storeSettlementMapper.selectByPeriodId(1L)).thenAnswer(invocation ->
                savedSettlement.get() == null ? List.of() : List.of(savedSettlement.get())
        );
        when(consumptionOrderMapper.selectEligibleForSettlement(any(), any()))
                .thenReturn(List.of(order));
        when(storeMapper.selectById(2L)).thenReturn(store);
        when(storeSettlementMapper.insert(any(StoreSettlement.class))).thenAnswer(invocation -> {
            StoreSettlement settlement = invocation.getArgument(0);
            settlement.setId(20L);
            savedSettlement.set(settlement);
            return 1;
        });
        when(storeSettlementItemMapper.insert(any(StoreSettlementItem.class))).thenReturn(1);
        when(consumptionOrderMapper.markIncludedInSettlement(10L)).thenReturn(1);
        when(settlementPeriodMapper.markGenerated(1L, NOW)).thenReturn(1);
        when(auditLogMapper.insert(any(AuditLog.class))).thenReturn(1);
        when(settlementPeriodMapper.selectByIds(anyCollection())).thenReturn(List.of(period));
        when(storeMapper.selectByIds(anyCollection())).thenReturn(List.of(store));

        var result = service.generate(new SettlementGenerateCommand("2026-08", 5L, "192.0.2.10"));

        assertEquals(SettlementPeriodStatus.GENERATED, result.periodStatus());
        assertEquals(1, result.settlementCount());
        assertEquals(100L, result.settlements().getFirst().totalConsumePoints());
        assertEquals(10_000L, result.settlements().getFirst().grossAmountCent());
        assertEquals(500L, result.settlements().getFirst().platformFeeCent());
        assertEquals(9_500L, result.settlements().getFirst().payableAmountCent());

        ArgumentCaptor<StoreSettlementItem> itemCaptor =
                ArgumentCaptor.forClass(StoreSettlementItem.class);
        verify(storeSettlementItemMapper).insert(itemCaptor.capture());
        assertEquals(SettlementItemType.CONSUMPTION, itemCaptor.getValue().getItemType());
        assertEquals(10L, itemCaptor.getValue().getConsumptionOrderId());
        verify(consumptionOrderMapper).markIncludedInSettlement(10L);
    }

    @Test
    void shouldGenerateNegativeAdjustmentForPreviouslySettledReversal() {
        allowSuperAdmin();
        SettlementPeriod period = openPeriod();
        Store store = store();
        ConsumptionOrder reversal = completedOrder();
        reversal.setOrderStatus(ConsumptionOrderStatus.REVERSED);
        reversal.setSettlementStatus(SettlementStatus.SETTLED);
        reversal.setReversedBy(5L);
        reversal.setReversedTime(LocalDateTime.of(2026, 8, 25, 12, 0));
        reversal.setReversalReason("重复扣减");
        AtomicReference<StoreSettlement> savedSettlement = new AtomicReference<>();

        when(settlementPeriodMapper.insertOpenPeriod(
                "2026-08",
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 31)
        )).thenReturn(1);
        when(settlementPeriodMapper.selectByPeriodCodeForUpdate("2026-08")).thenReturn(period);
        when(storeSettlementMapper.selectByPeriodId(1L)).thenAnswer(invocation ->
                savedSettlement.get() == null ? List.of() : List.of(savedSettlement.get())
        );
        when(consumptionOrderMapper.selectEligibleForSettlement(any(), any())).thenReturn(List.of());
        when(consumptionOrderMapper.selectEligibleReversalsForSettlement(any()))
                .thenReturn(List.of(reversal));
        when(storeMapper.selectById(2L)).thenReturn(store);
        when(storeSettlementMapper.insert(any(StoreSettlement.class))).thenAnswer(invocation -> {
            StoreSettlement settlement = invocation.getArgument(0);
            settlement.setId(20L);
            savedSettlement.set(settlement);
            return 1;
        });
        when(storeSettlementItemMapper.insert(any(StoreSettlementItem.class))).thenReturn(1);
        when(consumptionOrderMapper.markReversalAdjusted(10L)).thenReturn(1);
        when(settlementPeriodMapper.markGenerated(1L, NOW)).thenReturn(1);
        when(auditLogMapper.insert(any(AuditLog.class))).thenReturn(1);
        when(settlementPeriodMapper.selectByIds(anyCollection())).thenReturn(List.of(period));
        when(storeMapper.selectByIds(anyCollection())).thenReturn(List.of(store));

        var result = service.generate(new SettlementGenerateCommand("2026-08", 5L, null));

        var summary = result.settlements().getFirst();
        assertEquals(-100L, summary.totalConsumePoints());
        assertEquals(-10_000L, summary.grossAmountCent());
        assertEquals(-500L, summary.platformFeeCent());
        assertEquals(-9_500L, summary.payableAmountCent());

        ArgumentCaptor<StoreSettlementItem> itemCaptor =
                ArgumentCaptor.forClass(StoreSettlementItem.class);
        verify(storeSettlementItemMapper).insert(itemCaptor.capture());
        assertEquals(SettlementItemType.REVERSAL_ADJUSTMENT, itemCaptor.getValue().getItemType());
        assertEquals(-100L, itemCaptor.getValue().getPointsDelta());
        assertEquals(-9_500L, itemCaptor.getValue().getStorePayableCent());
        assertEquals("重复扣减", itemCaptor.getValue().getAdjustmentReason());
        verify(consumptionOrderMapper).markReversalAdjusted(10L);
    }

    @Test
    void shouldRejectCurrentMonthGeneration() {
        allowSuperAdmin();

        CustomException exception = assertThrows(CustomException.class, () ->
                service.generate(new SettlementGenerateCommand("2026-09", 5L, null))
        );

        assertEquals(ExceptionEnum.PLATFORM_SETTLEMENT_PERIOD_NOT_CLOSED.getCode(), exception.getCode());
        verify(settlementPeriodMapper, never()).insertOpenPeriod(any(), any(), any());
    }

    @Test
    void shouldReplayAlreadyGeneratedPeriodWithoutIncludingOrdersAgain() {
        allowSuperAdmin();
        SettlementPeriod period = openPeriod();
        period.setPeriodStatus(SettlementPeriodStatus.GENERATED);
        period.setGeneratedTime(NOW);
        StoreSettlement settlement = settlement(StoreSettlementStatus.GENERATED, null);
        when(settlementPeriodMapper.selectByPeriodCodeForUpdate("2026-08")).thenReturn(period);
        when(storeSettlementMapper.selectByPeriodId(1L)).thenReturn(List.of(settlement));
        when(settlementPeriodMapper.selectByIds(anyCollection())).thenReturn(List.of(period));
        when(storeMapper.selectByIds(anyCollection())).thenReturn(List.of(store()));

        var result = service.generate(new SettlementGenerateCommand("2026-08", 5L, null));

        assertEquals(1, result.settlementCount());
        assertEquals("STL100", result.settlements().getFirst().settlementNo());
        verify(consumptionOrderMapper, never()).selectEligibleForSettlement(any(), any());
        verify(storeSettlementMapper, never()).insert(any(StoreSettlement.class));
    }

    @Test
    void shouldRejectGenerationWhenMonthHasNoEligibleOrders() {
        allowSuperAdmin();
        when(settlementPeriodMapper.insertOpenPeriod(any(), any(), any())).thenReturn(1);
        when(settlementPeriodMapper.selectByPeriodCodeForUpdate("2026-08")).thenReturn(openPeriod());
        when(storeSettlementMapper.selectByPeriodId(1L)).thenReturn(List.of());
        when(consumptionOrderMapper.selectEligibleForSettlement(any(), any())).thenReturn(List.of());

        CustomException exception = assertThrows(CustomException.class, () ->
                service.generate(new SettlementGenerateCommand("2026-08", 5L, null))
        );

        assertEquals(ExceptionEnum.PLATFORM_SETTLEMENT_NO_ELIGIBLE_ORDERS.getCode(), exception.getCode());
        verify(storeSettlementMapper, never()).insert(any(StoreSettlement.class));
    }

    @Test
    void shouldFilterSettlementListByRequestedStoreForSuperAdmin() {
        allowSuperAdmin();
        StoreSettlement settlement = settlement(StoreSettlementStatus.GENERATED, null);
        Page<StoreSettlement> page = new Page<>(1, 20);
        page.setTotal(1);
        page.setRecords(List.of(settlement));
        when(storeSettlementMapper.selectPage(
                ArgumentMatchers.<Page<StoreSettlement>>any(),
                ArgumentMatchers.<Wrapper<StoreSettlement>>any()
        )).thenReturn(page);
        when(settlementPeriodMapper.selectByIds(anyCollection())).thenReturn(List.of(openPeriod()));
        when(storeMapper.selectByIds(anyCollection())).thenReturn(List.of(store()));

        var result = service.list(5L, 1, 20, null, null, 2L);

        assertEquals(1L, result.total());
        assertEquals(2L, result.items().getFirst().storeId());
    }

    @Test
    void shouldRejectSettlementListStoreOutsideManagerScope() {
        allowManager(2L);

        CustomException exception = assertThrows(CustomException.class, () ->
                service.list(6L, 1, 20, null, null, 3L)
        );

        assertEquals(ExceptionEnum.PLATFORM_SETTLEMENT_ACCESS_DENIED.getCode(), exception.getCode());
        verify(storeSettlementMapper, never()).selectPage(any(), any());
    }

    @Test
    void shouldAllowAssignedManagerToConfirmSettlement() {
        allowManager(2L);
        StoreSettlement generated = settlement(StoreSettlementStatus.GENERATED, null);
        StoreSettlement confirmed = settlement(StoreSettlementStatus.CONFIRMED, null);
        confirmed.setConfirmedTime(NOW);
        when(storeSettlementMapper.selectBySettlementNoForUpdate("STL100")).thenReturn(generated);
        when(storeSettlementMapper.markConfirmed(20L, NOW, "金额无误")).thenReturn(1);
        when(auditLogMapper.insert(any(AuditLog.class))).thenReturn(1);
        when(storeSettlementMapper.selectBySettlementNo("STL100")).thenReturn(confirmed);
        stubDetailDependencies();

        var result = service.confirm(new SettlementConfirmCommand(
                "STL100",
                6L,
                "金额无误",
                "192.0.2.11"
        ));

        assertEquals(StoreSettlementStatus.CONFIRMED, result.settlement().settlementStatus());
        verify(settlementPeriodMapper).markConfirmedIfAll(1L);
        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogMapper).insert(auditCaptor.capture());
        assertEquals(RoleCode.STORE_MANAGER, auditCaptor.getValue().getOperatorRole());
    }

    @Test
    void shouldDenyManagerFromOtherStoreSettlement() {
        allowManager(3L);
        when(storeSettlementMapper.selectBySettlementNoForUpdate("STL100"))
                .thenReturn(settlement(StoreSettlementStatus.GENERATED, null));

        CustomException exception = assertThrows(CustomException.class, () ->
                service.confirm(new SettlementConfirmCommand("STL100", 6L, null, null))
        );

        assertEquals(ExceptionEnum.PLATFORM_SETTLEMENT_ACCESS_DENIED.getCode(), exception.getCode());
        verify(storeSettlementMapper, never()).markConfirmed(any(), any(), any());
    }

    @Test
    void shouldMarkConfirmedSettlementPaidAndCloseItsConsumptionItems() {
        allowSuperAdmin();
        StoreSettlement confirmed = settlement(StoreSettlementStatus.CONFIRMED, null);
        StoreSettlement paid = settlement(StoreSettlementStatus.PAID, "BANK-SETTLEMENT-001");
        paid.setPaidTime(NOW);
        when(storeSettlementMapper.selectBySettlementNoForUpdate("STL100")).thenReturn(confirmed);
        when(storeSettlementItemMapper.selectCount(any())).thenReturn(1L);
        when(storeSettlementMapper.markPaid(
                20L,
                NOW,
                "BANK-SETTLEMENT-001",
                "已转账"
        )).thenReturn(1);
        when(consumptionOrderMapper.markSettlementItemsSettled(20L)).thenReturn(1);
        when(auditLogMapper.insert(any(AuditLog.class))).thenReturn(1);
        when(storeSettlementMapper.selectBySettlementNo("STL100")).thenReturn(paid);
        stubDetailDependencies();

        var result = service.markPaid(new SettlementPaymentCommand(
                "STL100",
                5L,
                "BANK-SETTLEMENT-001",
                "已转账",
                "192.0.2.12"
        ));

        assertEquals(StoreSettlementStatus.PAID, result.settlement().settlementStatus());
        assertEquals("BANK-SETTLEMENT-001", result.settlement().paymentReference());
        verify(consumptionOrderMapper).markSettlementItemsSettled(20L);
        verify(settlementPeriodMapper).markPaidIfAll(1L);
    }

    @Test
    void shouldSettleAdjustmentOnlyStatementWithoutUpdatingOriginalOrderAgain() {
        allowSuperAdmin();
        StoreSettlement confirmed = settlement(StoreSettlementStatus.CONFIRMED, null);
        confirmed.setTotalConsumePoints(-100L);
        confirmed.setGrossAmountCent(-10_000L);
        confirmed.setPlatformFeeCent(-500L);
        confirmed.setPayableAmountCent(-9_500L);
        StoreSettlement paid = settlement(StoreSettlementStatus.PAID, "BANK-RECEIPT-001");
        paid.setPaidTime(NOW);
        when(storeSettlementMapper.selectBySettlementNoForUpdate("STL100")).thenReturn(confirmed);
        when(storeSettlementItemMapper.selectCount(any())).thenReturn(1L, 0L);
        when(storeSettlementMapper.markPaid(
                20L,
                NOW,
                "BANK-RECEIPT-001",
                "门店已退回"
        )).thenReturn(1);
        when(auditLogMapper.insert(any(AuditLog.class))).thenReturn(1);
        when(storeSettlementMapper.selectBySettlementNo("STL100")).thenReturn(paid);
        stubDetailDependencies();

        var result = service.markPaid(new SettlementPaymentCommand(
                "STL100",
                5L,
                "BANK-RECEIPT-001",
                "门店已退回",
                null
        ));

        assertEquals(StoreSettlementStatus.PAID, result.settlement().settlementStatus());
        verify(consumptionOrderMapper, never()).markSettlementItemsSettled(any());
        verify(settlementPeriodMapper).markPaidIfAll(1L);
    }

    @Test
    void shouldReplayPaidSettlementOnlyForSamePaymentReference() {
        allowSuperAdmin();
        StoreSettlement paid = settlement(StoreSettlementStatus.PAID, "BANK-SETTLEMENT-001");
        paid.setPaidTime(NOW);
        when(storeSettlementMapper.selectBySettlementNoForUpdate("STL100")).thenReturn(paid);
        stubDetailDependencies();

        var result = service.markPaid(new SettlementPaymentCommand(
                "STL100",
                5L,
                "BANK-SETTLEMENT-001",
                null,
                null
        ));

        assertEquals(StoreSettlementStatus.PAID, result.settlement().settlementStatus());
        verify(storeSettlementMapper, never()).markPaid(any(), any(), any(), any());
        verify(auditLogMapper, never()).insert(any(AuditLog.class));
    }

    private void allowSuperAdmin() {
        when(sysUserMapper.selectById(5L)).thenReturn(activeUser(5L));
        when(staffAuthorityMapper.selectRoleCodes(5L)).thenReturn(List.of(RoleCode.SUPER_ADMIN.getCode()));
        when(staffAuthorityMapper.selectStoreIds(5L)).thenReturn(List.of());
    }

    private void allowManager(Long storeId) {
        when(sysUserMapper.selectById(6L)).thenReturn(activeUser(6L));
        when(staffAuthorityMapper.selectRoleCodes(6L)).thenReturn(List.of(RoleCode.STORE_MANAGER.getCode()));
        when(staffAuthorityMapper.selectStoreIds(6L)).thenReturn(List.of(storeId));
    }

    private SysUser activeUser(Long id) {
        return SysUser.builder().id(id).status(SysUserStatus.ACTIVE).build();
    }

    private SettlementPeriod openPeriod() {
        return SettlementPeriod.builder()
                .id(1L)
                .periodCode("2026-08")
                .startDate(LocalDate.of(2026, 8, 1))
                .endDate(LocalDate.of(2026, 8, 31))
                .periodStatus(SettlementPeriodStatus.OPEN)
                .build();
    }

    private Store store() {
        return Store.builder().id(2L).merchantId(3L).storeName("一号门店").build();
    }

    private ConsumptionOrder completedOrder() {
        return ConsumptionOrder.builder()
                .id(10L)
                .orderNo("CSM100")
                .customerId(7L)
                .storeId(2L)
                .consumePoints(100L)
                .grossAmountCent(10_000L)
                .platformFeeCent(500L)
                .storePayableCent(9_500L)
                .orderStatus(ConsumptionOrderStatus.COMPLETED)
                .settlementStatus(SettlementStatus.NOT_INCLUDED)
                .completedTime(LocalDateTime.of(2026, 8, 20, 12, 0))
                .build();
    }

    private StoreSettlement settlement(StoreSettlementStatus status, String paymentReference) {
        return StoreSettlement.builder()
                .id(20L)
                .settlementNo("STL100")
                .periodId(1L)
                .merchantId(3L)
                .storeId(2L)
                .totalConsumePoints(100L)
                .grossAmountCent(10_000L)
                .platformFeeCent(500L)
                .adjustmentAmountCent(0L)
                .payableAmountCent(9_500L)
                .settlementStatus(status)
                .paymentReference(paymentReference)
                .build();
    }

    private void stubDetailDependencies() {
        SettlementPeriod period = openPeriod();
        period.setPeriodStatus(SettlementPeriodStatus.GENERATED);
        period.setGeneratedTime(NOW);
        when(settlementPeriodMapper.selectByIds(anyCollection())).thenReturn(List.of(period));
        when(storeMapper.selectByIds(anyCollection())).thenReturn(List.of(store()));
        when(storeSettlementItemMapper.selectList(any())).thenReturn(List.of());
    }
}
