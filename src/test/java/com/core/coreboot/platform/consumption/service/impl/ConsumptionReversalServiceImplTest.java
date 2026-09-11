package com.core.coreboot.platform.consumption.service.impl;

import com.core.coreboot.exception.CustomException;
import com.core.coreboot.exception.ExceptionEnum;
import com.core.coreboot.platform.audit.entity.AuditLog;
import com.core.coreboot.platform.audit.mapper.AuditLogMapper;
import com.core.coreboot.platform.common.enums.ConsumptionOrderStatus;
import com.core.coreboot.platform.common.enums.PointLedgerBusinessType;
import com.core.coreboot.platform.common.enums.PointLedgerType;
import com.core.coreboot.platform.common.enums.PointLotStatus;
import com.core.coreboot.platform.common.enums.RoleCode;
import com.core.coreboot.platform.common.enums.SettlementStatus;
import com.core.coreboot.platform.common.enums.SysUserStatus;
import com.core.coreboot.platform.consumption.entity.ConsumptionOrder;
import com.core.coreboot.platform.consumption.mapper.ConsumptionOrderMapper;
import com.core.coreboot.platform.consumption.model.ConsumptionReversalCommand;
import com.core.coreboot.platform.point.entity.PointAccount;
import com.core.coreboot.platform.point.entity.PointLedger;
import com.core.coreboot.platform.point.entity.PointLot;
import com.core.coreboot.platform.point.entity.PointLotUsage;
import com.core.coreboot.platform.point.mapper.PointAccountMapper;
import com.core.coreboot.platform.point.mapper.PointLedgerMapper;
import com.core.coreboot.platform.point.mapper.PointLotMapper;
import com.core.coreboot.platform.point.mapper.PointLotUsageMapper;
import com.core.coreboot.platform.staff.entity.SysUser;
import com.core.coreboot.platform.staff.mapper.StaffAuthorityMapper;
import com.core.coreboot.platform.staff.mapper.SysUserMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsumptionReversalServiceImplTest {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 11, 9, 0);

    @Mock
    private ConsumptionOrderMapper consumptionOrderMapper;
    @Mock
    private PointAccountMapper pointAccountMapper;
    @Mock
    private PointLotMapper pointLotMapper;
    @Mock
    private PointLotUsageMapper pointLotUsageMapper;
    @Mock
    private PointLedgerMapper pointLedgerMapper;
    @Mock
    private SysUserMapper sysUserMapper;
    @Mock
    private StaffAuthorityMapper staffAuthorityMapper;
    @Mock
    private AuditLogMapper auditLogMapper;

    private ConsumptionReversalServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ConsumptionReversalServiceImpl(
                consumptionOrderMapper,
                pointAccountMapper,
                pointLotMapper,
                pointLotUsageMapper,
                pointLedgerMapper,
                sysUserMapper,
                staffAuthorityMapper,
                auditLogMapper,
                new ObjectMapper().findAndRegisterModules(),
                Clock.fixed(Instant.parse("2026-09-11T09:00:00Z"), ZoneOffset.UTC)
        );
    }

    @Test
    void shouldReverseConsumptionRestoreOriginalLotsAndWriteImmutableRecords() {
        allowSuperAdmin();
        ConsumptionOrder order = completedOrder(SettlementStatus.NOT_INCLUDED);
        PointAccount account = account();
        PointLotUsage usage1 = usage(101L, 201L, 30L);
        PointLotUsage usage2 = usage(102L, 202L, 20L);
        when(consumptionOrderMapper.selectByOrderNo("CSM-001")).thenReturn(order);
        when(pointAccountMapper.selectByCustomerIdForUpdate(7L)).thenReturn(account);
        when(consumptionOrderMapper.selectByOrderNoForUpdate("CSM-001")).thenReturn(order);
        when(pointLotUsageMapper.selectByConsumptionOrderIdForUpdate(10L))
                .thenReturn(List.of(usage1, usage2));
        when(pointLotMapper.selectByIdForUpdate(201L)).thenReturn(lot(201L, 30L, 0L));
        when(pointLotMapper.selectByIdForUpdate(202L)).thenReturn(lot(202L, 100L, 40L));
        when(pointLotMapper.restorePoints(any(), any(), any())).thenReturn(1);
        when(pointLotUsageMapper.markFullyReversed(any(), any(), any())).thenReturn(1);
        when(pointAccountMapper.increaseBalance(80L, 50L)).thenReturn(1);
        when(consumptionOrderMapper.markReversed(10L, 5L, NOW, "门店误操作"))
                .thenReturn(1);
        when(pointLedgerMapper.insert(any(PointLedger.class))).thenReturn(1);
        when(auditLogMapper.insert(any(AuditLog.class))).thenReturn(1);

        var result = service.reverse(command("门店误操作", "reverse-001"));

        assertEquals(50L, result.reversedPoints());
        assertEquals(120L, result.availablePoints());
        assertEquals(SettlementStatus.ADJUSTED, result.settlementStatus());
        assertEquals(NOW, result.reversedTime());
        verify(pointLotMapper).restorePoints(201L, 7L, 30L);
        verify(pointLotMapper).restorePoints(202L, 7L, 20L);
        verify(pointLotUsageMapper).markFullyReversed(101L, 10L, 30L);
        verify(pointLotUsageMapper).markFullyReversed(102L, 10L, 20L);

        ArgumentCaptor<PointLedger> ledgerCaptor = ArgumentCaptor.forClass(PointLedger.class);
        verify(pointLedgerMapper).insert(ledgerCaptor.capture());
        PointLedger ledger = ledgerCaptor.getValue();
        assertEquals(50L, ledger.getDeltaPoints());
        assertEquals(120L, ledger.getBalanceAfter());
        assertEquals(PointLedgerType.REVERSAL, ledger.getLedgerType());
        assertEquals(PointLedgerBusinessType.CONSUMPTION_ORDER, ledger.getBusinessType());
        assertEquals("CSM-001", ledger.getBusinessNo());

        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogMapper).insert(auditCaptor.capture());
        assertEquals("CONSUMPTION_ORDER_REVERSED", auditCaptor.getValue().getAction());
        assertEquals("门店误操作", auditCaptor.getValue().getRemark());
        assertTrue(auditCaptor.getValue().getAfterSnapshot().contains("\"balance\":120"));
    }

    @Test
    void shouldKeepSettledOrderPendingForFutureSettlementAdjustment() {
        allowSuperAdmin();
        ConsumptionOrder order = completedOrder(SettlementStatus.SETTLED);
        stubSingleUsageReversal(order);

        var result = service.reverse(command("重复扣减", "reverse-002"));

        assertEquals(SettlementStatus.SETTLED, result.settlementStatus());
        assertEquals(ConsumptionOrderStatus.REVERSED, order.getOrderStatus());
    }

    @Test
    void shouldReplaySameIdempotentRequestWithoutRestoringPointsAgain() {
        allowSuperAdmin();
        ConsumptionOrder order = completedOrder(SettlementStatus.ADJUSTED);
        order.setOrderStatus(ConsumptionOrderStatus.REVERSED);
        order.setReversedBy(5L);
        order.setReversedTime(NOW);
        order.setReversalReason("门店误操作");
        PointLedger ledger = PointLedger.builder()
                .ledgerNo("LDG-001")
                .customerId(7L)
                .storeId(2L)
                .deltaPoints(50L)
                .balanceAfter(120L)
                .ledgerType(PointLedgerType.REVERSAL)
                .businessType(PointLedgerBusinessType.CONSUMPTION_ORDER)
                .businessNo("CSM-001")
                .operatorId(5L)
                .idempotencyKey("reverse-001")
                .remark("门店误操作")
                .build();
        when(pointLedgerMapper.selectByIdempotencyKey("reverse-001")).thenReturn(ledger);
        when(consumptionOrderMapper.selectByOrderNo("CSM-001")).thenReturn(order);
        PointAccount replayAccount = account();
        replayAccount.setAvailablePoints(120L);
        when(pointAccountMapper.selectByCustomerId(7L)).thenReturn(replayAccount);

        var result = service.reverse(command("门店误操作", "reverse-001"));

        assertEquals("LDG-001", result.ledgerNo());
        assertEquals(120L, result.availablePoints());
        verify(pointAccountMapper, never()).increaseBalance(any(), any());
        verify(pointLotMapper, never()).restorePoints(any(), any(), any());
    }

    @Test
    void shouldRejectIdempotencyKeyReusedForDifferentRequest() {
        allowSuperAdmin();
        PointLedger ledger = PointLedger.builder()
                .ledgerType(PointLedgerType.RECHARGE)
                .businessType(PointLedgerBusinessType.RECHARGE_ORDER)
                .businessNo("RCH-001")
                .build();
        when(pointLedgerMapper.selectByIdempotencyKey("reverse-001")).thenReturn(ledger);

        CustomException exception = assertThrows(
                CustomException.class,
                () -> service.reverse(command("门店误操作", "reverse-001"))
        );

        assertEquals(ExceptionEnum.PLATFORM_IDEMPOTENCY_CONFLICT.getCode(), exception.getCode());
        verify(pointAccountMapper, never()).increaseBalance(any(), any());
    }

    @Test
    void shouldRejectPendingConsumptionOrder() {
        allowSuperAdmin();
        ConsumptionOrder order = completedOrder(SettlementStatus.NOT_INCLUDED);
        order.setOrderStatus(ConsumptionOrderStatus.PENDING_CONFIRM);
        when(consumptionOrderMapper.selectByOrderNo("CSM-001")).thenReturn(order);
        when(pointAccountMapper.selectByCustomerIdForUpdate(7L)).thenReturn(account());
        when(consumptionOrderMapper.selectByOrderNoForUpdate("CSM-001")).thenReturn(order);

        CustomException exception = assertThrows(
                CustomException.class,
                () -> service.reverse(command("门店误操作", "reverse-003"))
        );

        assertEquals(
                ExceptionEnum.PLATFORM_CONSUMPTION_REVERSAL_NOT_ALLOWED.getCode(),
                exception.getCode()
        );
        verify(pointLotUsageMapper, never()).selectByConsumptionOrderIdForUpdate(any());
    }

    @Test
    void shouldRejectUsageTotalsThatDoNotMatchOrder() {
        allowSuperAdmin();
        ConsumptionOrder order = completedOrder(SettlementStatus.NOT_INCLUDED);
        when(consumptionOrderMapper.selectByOrderNo("CSM-001")).thenReturn(order);
        when(pointAccountMapper.selectByCustomerIdForUpdate(7L)).thenReturn(account());
        when(consumptionOrderMapper.selectByOrderNoForUpdate("CSM-001")).thenReturn(order);
        when(pointLotUsageMapper.selectByConsumptionOrderIdForUpdate(10L))
                .thenReturn(List.of(usage(101L, 201L, 49L)));

        CustomException exception = assertThrows(
                CustomException.class,
                () -> service.reverse(command("门店误操作", "reverse-004"))
        );

        assertEquals(
                ExceptionEnum.PLATFORM_CONSUMPTION_REVERSAL_DATA_INVALID.getCode(),
                exception.getCode()
        );
        verify(pointLotMapper, never()).restorePoints(any(), any(), any());
    }

    private void stubSingleUsageReversal(ConsumptionOrder order) {
        when(consumptionOrderMapper.selectByOrderNo("CSM-001")).thenReturn(order);
        when(pointAccountMapper.selectByCustomerIdForUpdate(7L)).thenReturn(account());
        when(consumptionOrderMapper.selectByOrderNoForUpdate("CSM-001")).thenReturn(order);
        when(pointLotUsageMapper.selectByConsumptionOrderIdForUpdate(10L))
                .thenReturn(List.of(usage(101L, 201L, 50L)));
        when(pointLotMapper.selectByIdForUpdate(201L)).thenReturn(lot(201L, 50L, 0L));
        when(pointLotMapper.restorePoints(201L, 7L, 50L)).thenReturn(1);
        when(pointLotUsageMapper.markFullyReversed(101L, 10L, 50L)).thenReturn(1);
        when(pointAccountMapper.increaseBalance(80L, 50L)).thenReturn(1);
        when(consumptionOrderMapper.markReversed(10L, 5L, NOW, "重复扣减")).thenReturn(1);
        when(pointLedgerMapper.insert(any(PointLedger.class))).thenReturn(1);
        when(auditLogMapper.insert(any(AuditLog.class))).thenReturn(1);
    }

    private void allowSuperAdmin() {
        when(sysUserMapper.selectById(5L)).thenReturn(SysUser.builder()
                .id(5L)
                .status(SysUserStatus.ACTIVE)
                .build());
        when(staffAuthorityMapper.selectRoleCodes(5L))
                .thenReturn(List.of(RoleCode.SUPER_ADMIN.getCode()));
    }

    private ConsumptionReversalCommand command(String reason, String idempotencyKey) {
        return new ConsumptionReversalCommand(
                "CSM-001",
                5L,
                reason,
                idempotencyKey,
                "192.0.2.20"
        );
    }

    private ConsumptionOrder completedOrder(SettlementStatus settlementStatus) {
        return ConsumptionOrder.builder()
                .id(10L)
                .orderNo("CSM-001")
                .customerId(7L)
                .storeId(2L)
                .consumePoints(50L)
                .grossAmountCent(5_000L)
                .platformFeeCent(250L)
                .storePayableCent(4_750L)
                .orderStatus(ConsumptionOrderStatus.COMPLETED)
                .settlementStatus(settlementStatus)
                .completedTime(NOW.minusDays(1))
                .build();
    }

    private PointAccount account() {
        return PointAccount.builder()
                .id(80L)
                .customerId(7L)
                .availablePoints(70L)
                .build();
    }

    private PointLotUsage usage(Long usageId, Long lotId, Long points) {
        return PointLotUsage.builder()
                .id(usageId)
                .consumptionOrderId(10L)
                .pointLotId(lotId)
                .usedPoints(points)
                .reversedPoints(0L)
                .build();
    }

    private PointLot lot(Long lotId, Long totalPoints, Long remainingPoints) {
        return PointLot.builder()
                .id(lotId)
                .customerId(7L)
                .totalPoints(totalPoints)
                .remainingPoints(remainingPoints)
                .lotStatus(remainingPoints == 0L ? PointLotStatus.DEPLETED : PointLotStatus.AVAILABLE)
                .build();
    }
}
