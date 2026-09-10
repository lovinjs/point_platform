package com.core.coreboot.platform.finance.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.core.coreboot.exception.CustomException;
import com.core.coreboot.exception.ExceptionEnum;
import com.core.coreboot.platform.audit.entity.AuditLog;
import com.core.coreboot.platform.audit.mapper.AuditLogMapper;
import com.core.coreboot.platform.common.enums.AuditActorType;
import com.core.coreboot.platform.common.enums.RoleCode;
import com.core.coreboot.platform.common.enums.StoreStatus;
import com.core.coreboot.platform.common.enums.SysUserStatus;
import com.core.coreboot.platform.customer.entity.CustomerUser;
import com.core.coreboot.platform.customer.mapper.CustomerUserMapper;
import com.core.coreboot.platform.finance.mapper.FinancialReconciliationMapper;
import com.core.coreboot.platform.finance.model.FinancialStoreAggregate;
import com.core.coreboot.platform.merchant.entity.Store;
import com.core.coreboot.platform.merchant.mapper.StoreMapper;
import com.core.coreboot.platform.staff.entity.SysUser;
import com.core.coreboot.platform.staff.mapper.StaffAuthorityMapper;
import com.core.coreboot.platform.staff.mapper.SysUserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminFinanceServiceImplTest {
    private static final Long OPERATOR_ID = 9L;
    private static final LocalDate START_DATE = LocalDate.of(2026, 9, 1);
    private static final LocalDate END_DATE = LocalDate.of(2026, 9, 10);

    @Mock
    private FinancialReconciliationMapper reconciliationMapper;
    @Mock
    private AuditLogMapper auditLogMapper;
    @Mock
    private StoreMapper storeMapper;
    @Mock
    private SysUserMapper sysUserMapper;
    @Mock
    private CustomerUserMapper customerUserMapper;
    @Mock
    private StaffAuthorityMapper staffAuthorityMapper;

    private AdminFinanceServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AdminFinanceServiceImpl(
                reconciliationMapper,
                auditLogMapper,
                storeMapper,
                sysUserMapper,
                customerUserMapper,
                staffAuthorityMapper,
                Clock.fixed(Instant.parse("2026-09-10T02:00:00Z"), ZoneId.of("Asia/Shanghai"))
        );
    }

    @Test
    void shouldReconcileIndependentCashConsumptionAndSettlementEvents() {
        allowSuperAdmin();
        when(reconciliationMapper.selectRechargeAggregates(
                START_DATE.atStartOfDay(), END_DATE.plusDays(1).atStartOfDay(), null
        )).thenReturn(List.of(rechargeAggregate(2L, 100_000L, 1_000L)));
        when(reconciliationMapper.selectRefundAggregates(
                START_DATE.atStartOfDay(), END_DATE.plusDays(1).atStartOfDay(), null
        )).thenReturn(List.of(refundAggregate(2L, 20_000L, 200L)));
        when(reconciliationMapper.selectConsumptionAggregates(
                START_DATE.atStartOfDay(), END_DATE.plusDays(1).atStartOfDay(), null
        )).thenReturn(List.of(consumptionAggregate(2L)));
        when(reconciliationMapper.selectSettlementPaymentAggregates(
                START_DATE.atStartOfDay(), END_DATE.plusDays(1).atStartOfDay(), null
        )).thenReturn(List.of(settlementAggregate(2L, 9_500L)));
        when(storeMapper.selectByIds(List.of(2L))).thenReturn(List.of(store(2L)));

        var result = service.reconcile(OPERATOR_ID, START_DATE, END_DATE, null);

        assertEquals(1, result.stores().size());
        var row = result.stores().getFirst();
        assertEquals(100_000L, row.rechargeReceiptCent());
        assertEquals(20_000L, row.refundOutflowCent());
        assertEquals(80_000L, row.netRechargeCashCent());
        assertEquals(30_000L, row.consumptionGrossCent());
        assertEquals(1_500L, row.platformFeeCent());
        assertEquals(28_500L, row.storePayableCent());
        assertEquals(9_500L, row.notIncludedPayableCent());
        assertEquals(9_500L, row.includedPayableCent());
        assertEquals(9_500L, row.settledPayableCent());
        assertEquals(9_500L, row.settlementPaidCent());
        assertEquals(row.netRechargeCashCent(), result.summary().netRechargeCashCent());
        assertEquals(row.storePayableCent(), result.summary().storePayableCent());
    }

    @Test
    void shouldReturnSelectedStoreWithZeroValuesWhenPeriodHasNoActivity() {
        allowSuperAdmin();
        Store selectedStore = store(3L);
        selectedStore.setStatus(StoreStatus.CLOSED);
        when(storeMapper.selectById(3L)).thenReturn(selectedStore);

        var result = service.reconcile(OPERATOR_ID, START_DATE, END_DATE, 3L);

        assertEquals(1, result.stores().size());
        assertEquals(3L, result.stores().getFirst().storeId());
        assertEquals(StoreStatus.CLOSED, result.stores().getFirst().storeStatus());
        assertEquals(0L, result.summary().rechargeReceiptCent());
        assertEquals(0L, result.summary().consumptionGrossCent());
        assertEquals(0L, result.summary().settlementPaidCent());
    }

    @Test
    void shouldRejectFutureAndOversizedDateRangesBeforeFinanceQueries() {
        allowSuperAdmin();

        CustomException future = assertThrows(
                CustomException.class,
                () -> service.reconcile(
                        OPERATOR_ID,
                        LocalDate.of(2026, 9, 1),
                        LocalDate.of(2026, 9, 11),
                        null
                )
        );
        CustomException tooLong = assertThrows(
                CustomException.class,
                () -> service.reconcile(
                        OPERATOR_ID,
                        LocalDate.of(2025, 9, 9),
                        LocalDate.of(2026, 9, 10),
                        null
                )
        );

        assertEquals(ExceptionEnum.PLATFORM_INVALID_REQUEST.getCode(), future.getCode());
        assertEquals(ExceptionEnum.PLATFORM_INVALID_REQUEST.getCode(), tooLong.getCode());
        verify(reconciliationMapper, never()).selectRechargeAggregates(
                ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()
        );
    }

    @Test
    void shouldRejectNonSuperAdminBeforeFinanceQueries() {
        when(sysUserMapper.selectById(OPERATOR_ID)).thenReturn(activeOperator());
        when(staffAuthorityMapper.selectRoleCodes(OPERATOR_ID)).thenReturn(List.of("STORE_MANAGER"));

        CustomException exception = assertThrows(
                CustomException.class,
                () -> service.reconcile(OPERATOR_ID, START_DATE, END_DATE, null)
        );

        assertEquals(ExceptionEnum.PLATFORM_ADMIN_ACCESS_DENIED.getCode(), exception.getCode());
        verify(reconciliationMapper, never()).selectRechargeAggregates(
                ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()
        );
    }

    @Test
    void shouldEnrichAuditLogsAndExposeTraceDetails() {
        allowSuperAdmin();
        AuditLog log = AuditLog.builder()
                .id(21L)
                .actorType(AuditActorType.SYS_USER)
                .actorId(12L)
                .operatorRole(RoleCode.SUPER_ADMIN)
                .storeId(2L)
                .action("RECHARGE_REFUND_COMPLETED")
                .resourceType("RECHARGE_ORDER")
                .resourceNo("RCH21")
                .requestId("REQ-21")
                .beforeSnapshot("{\"status\":\"COMPLETED\"}")
                .afterSnapshot("{\"status\":\"REFUNDED\"}")
                .remark("全额退款")
                .clientIp("192.0.2.21")
                .createTime(LocalDateTime.of(2026, 9, 9, 15, 30))
                .build();
        when(auditLogMapper.selectPage(
                ArgumentMatchers.<Page<AuditLog>>any(),
                ArgumentMatchers.<Wrapper<AuditLog>>any()
        )).thenReturn(pageOf(1, 20, 1, List.of(log)));
        when(sysUserMapper.selectByIds(List.of(12L))).thenReturn(List.of(
                SysUser.builder().id(12L).username("finance").realName("财务管理员").build()
        ));
        when(storeMapper.selectByIds(List.of(2L))).thenReturn(List.of(store(2L)));

        var result = service.listAuditLogs(
                OPERATOR_ID,
                1,
                20,
                START_DATE,
                END_DATE,
                2L,
                AuditActorType.SYS_USER,
                " RECHARGE_REFUND_COMPLETED ",
                " RCH21 "
        );

        assertEquals(1, result.total());
        var item = result.items().getFirst();
        assertEquals("财务管理员", item.actorName());
        assertEquals("测试门店2", item.storeName());
        assertEquals("REQ-21", item.requestId());
        assertEquals("{\"status\":\"COMPLETED\"}", item.beforeSnapshot());
        assertEquals("{\"status\":\"REFUNDED\"}", item.afterSnapshot());
    }

    @Test
    void shouldNormalizeAvailableAuditActions() {
        allowSuperAdmin();
        when(auditLogMapper.selectDistinctActions()).thenReturn(List.of(
                " STORE_UPDATED ",
                "CONSUMPTION_COMPLETED",
                "STORE_UPDATED",
                " "
        ));

        var result = service.listAuditActions(OPERATOR_ID);

        assertEquals(List.of("CONSUMPTION_COMPLETED", "STORE_UPDATED"), result);
    }

    private void allowSuperAdmin() {
        when(sysUserMapper.selectById(OPERATOR_ID)).thenReturn(activeOperator());
        when(staffAuthorityMapper.selectRoleCodes(OPERATOR_ID)).thenReturn(List.of("SUPER_ADMIN"));
    }

    private SysUser activeOperator() {
        return SysUser.builder().id(OPERATOR_ID).status(SysUserStatus.ACTIVE).build();
    }

    private Store store(Long id) {
        return Store.builder()
                .id(id)
                .storeCode("STORE-" + id)
                .storeName("测试门店" + id)
                .status(StoreStatus.ACTIVE)
                .build();
    }

    private FinancialStoreAggregate rechargeAggregate(Long storeId, long amountCent, long points) {
        FinancialStoreAggregate aggregate = aggregate(storeId);
        aggregate.setRechargeReceiptCent(amountCent);
        aggregate.setRechargePoints(points);
        return aggregate;
    }

    private FinancialStoreAggregate refundAggregate(Long storeId, long amountCent, long points) {
        FinancialStoreAggregate aggregate = aggregate(storeId);
        aggregate.setRefundOutflowCent(amountCent);
        aggregate.setRefundPoints(points);
        return aggregate;
    }

    private FinancialStoreAggregate consumptionAggregate(Long storeId) {
        FinancialStoreAggregate aggregate = aggregate(storeId);
        aggregate.setConsumptionGrossCent(30_000L);
        aggregate.setConsumptionPoints(300L);
        aggregate.setPlatformFeeCent(1_500L);
        aggregate.setStorePayableCent(28_500L);
        aggregate.setNotIncludedPayableCent(9_500L);
        aggregate.setIncludedPayableCent(9_500L);
        aggregate.setSettledPayableCent(9_500L);
        return aggregate;
    }

    private FinancialStoreAggregate settlementAggregate(Long storeId, long paidCent) {
        FinancialStoreAggregate aggregate = aggregate(storeId);
        aggregate.setSettlementPaidCent(paidCent);
        return aggregate;
    }

    private FinancialStoreAggregate aggregate(Long storeId) {
        FinancialStoreAggregate aggregate = new FinancialStoreAggregate();
        aggregate.setStoreId(storeId);
        return aggregate;
    }

    private <T> Page<T> pageOf(long current, long size, long total, List<T> records) {
        Page<T> page = new Page<>(current, size);
        page.setTotal(total);
        page.setRecords(records);
        return page;
    }
}
