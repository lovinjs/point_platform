package com.core.coreboot.platform.dashboard.service.impl;

import com.core.coreboot.exception.CustomException;
import com.core.coreboot.exception.ExceptionEnum;
import com.core.coreboot.platform.common.enums.StoreStatus;
import com.core.coreboot.platform.common.enums.SysUserStatus;
import com.core.coreboot.platform.dashboard.mapper.DashboardOverviewMapper;
import com.core.coreboot.platform.dashboard.model.DashboardBacklogAggregate;
import com.core.coreboot.platform.dashboard.model.DashboardDailyAggregate;
import com.core.coreboot.platform.dashboard.model.DashboardPeriodAggregate;
import com.core.coreboot.platform.merchant.entity.Store;
import com.core.coreboot.platform.merchant.mapper.StoreMapper;
import com.core.coreboot.platform.staff.entity.SysUser;
import com.core.coreboot.platform.staff.mapper.StaffAuthorityMapper;
import com.core.coreboot.platform.staff.mapper.SysUserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminDashboardServiceImplTest {
    private static final Long OPERATOR_ID = 9L;
    private static final LocalDate TODAY = LocalDate.of(2026, 9, 10);
    private static final LocalDateTime END_TIME = LocalDateTime.of(2026, 9, 11, 0, 0);
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 10, 10, 0);

    @Mock
    private DashboardOverviewMapper dashboardMapper;
    @Mock
    private StoreMapper storeMapper;
    @Mock
    private SysUserMapper sysUserMapper;
    @Mock
    private StaffAuthorityMapper staffAuthorityMapper;

    private AdminDashboardServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AdminDashboardServiceImpl(
                dashboardMapper,
                storeMapper,
                sysUserMapper,
                staffAuthorityMapper,
                Clock.fixed(Instant.parse("2026-09-10T02:00:00Z"), ZoneId.of("Asia/Shanghai"))
        );
    }

    @Test
    void shouldBuildGlobalOverviewAndFillMissingTrendDates() {
        allowSuperAdmin();
        DashboardPeriodAggregate today = periodAggregate(100_000L, 20_000L, 30_000L);
        DashboardPeriodAggregate month = periodAggregate(500_000L, 40_000L, 200_000L);
        when(dashboardMapper.selectPeriodMetrics(TODAY.atStartOfDay(), END_TIME, null, null))
                .thenReturn(today);
        when(dashboardMapper.selectPeriodMetrics(
                LocalDate.of(2026, 9, 1).atStartOfDay(), END_TIME, null, null
        )).thenReturn(month);
        when(dashboardMapper.selectConsumptionBacklog(NOW, null, null))
                .thenReturn(consumptionBacklog());
        when(dashboardMapper.selectSettlementBacklog(null, null))
                .thenReturn(settlementBacklog());
        when(dashboardMapper.selectDailyMetrics(
                LocalDate.of(2026, 9, 4).atStartOfDay(), END_TIME, null, null
        )).thenReturn(List.of(dailyAggregate(LocalDate.of(2026, 9, 9))));

        var result = service.overview(OPERATOR_ID, null, 7);

        assertTrue(result.globalScope());
        assertEquals(80_000L, result.today().netRechargeCashCent());
        assertEquals(30_000L, result.today().consumptionGrossCent());
        assertEquals(1_500L, result.today().platformFeeCent());
        assertEquals(28_500L, result.today().storePayableCent());
        assertEquals(7, result.trend().size());
        assertEquals(LocalDate.of(2026, 9, 4), result.trend().getFirst().businessDate());
        assertEquals(LocalDate.of(2026, 9, 10), result.trend().getLast().businessDate());
        assertEquals(8_000L, result.trend().get(5).netRechargeCashCent());
        assertEquals(2L, result.backlog().pendingConsumptionCount());
        assertEquals(28_500L, result.backlog().notIncludedPayableCent());
        assertEquals(2L, result.backlog().awaitingPlatformPaymentCount());
        assertEquals(57_000L, result.backlog().awaitingPlatformPaymentCent());
    }

    @Test
    void shouldLetManagerQueryOnlyAnAssignedStore() {
        allowStoreManager(List.of(2L));
        Store store = Store.builder()
                .id(2L)
                .storeCode("STORE-2")
                .storeName("测试门店2")
                .status(StoreStatus.ACTIVE)
                .build();
        when(storeMapper.selectById(2L)).thenReturn(store);
        when(dashboardMapper.selectPeriodMetrics(any(), any(), any(), any())).thenReturn(null);
        when(dashboardMapper.selectConsumptionBacklog(any(), any(), any())).thenReturn(null);
        when(dashboardMapper.selectSettlementBacklog(any(), any())).thenReturn(null);
        when(dashboardMapper.selectDailyMetrics(any(), any(), any(), any())).thenReturn(List.of());

        var result = service.overview(OPERATOR_ID, 2L, 30);

        assertFalse(result.globalScope());
        assertEquals(2L, result.selectedStoreId());
        assertEquals("测试门店2", result.selectedStoreName());
        assertEquals(30, result.trend().size());
        verify(dashboardMapper).selectPeriodMetrics(TODAY.atStartOfDay(), END_TIME, 2L, null);
    }

    @Test
    void shouldRejectManagerStoreOutsideScopeBeforeReadingStore() {
        allowStoreManager(List.of(2L));

        CustomException exception = assertThrows(
                CustomException.class,
                () -> service.overview(OPERATOR_ID, 3L, 7)
        );

        assertEquals(ExceptionEnum.PLATFORM_STORE_ACCESS_DENIED.getCode(), exception.getCode());
        verify(storeMapper, never()).selectById(any());
        verify(dashboardMapper, never()).selectPeriodMetrics(any(), any(), any(), any());
    }

    @Test
    void shouldReturnZeroOverviewWithoutQueriesWhenManagerHasNoStores() {
        allowStoreManager(List.of());

        var result = service.overview(OPERATOR_ID, null, 7);

        assertFalse(result.globalScope());
        assertEquals(0, result.today().rechargeOrderCount());
        assertEquals(0, result.currentMonth().consumptionOrderCount());
        assertEquals(7, result.trend().size());
        verify(dashboardMapper, never()).selectPeriodMetrics(any(), any(), any(), any());
        verify(dashboardMapper, never()).selectDailyMetrics(any(), any(), any(), any());
    }

    @Test
    void shouldRejectClerkAndUnsupportedTrendDays() {
        when(sysUserMapper.selectById(OPERATOR_ID)).thenReturn(activeOperator());
        when(staffAuthorityMapper.selectRoleCodes(OPERATOR_ID)).thenReturn(List.of("CLERK"));

        CustomException denied = assertThrows(
                CustomException.class,
                () -> service.overview(OPERATOR_ID, null, 7)
        );
        assertEquals(ExceptionEnum.PLATFORM_ADMIN_ACCESS_DENIED.getCode(), denied.getCode());

        allowSuperAdmin();
        CustomException invalidDays = assertThrows(
                CustomException.class,
                () -> service.overview(OPERATOR_ID, null, 14)
        );
        assertEquals(ExceptionEnum.PLATFORM_INVALID_REQUEST.getCode(), invalidDays.getCode());
    }

    private DashboardPeriodAggregate periodAggregate(long receipt, long refunds, long consumption) {
        DashboardPeriodAggregate aggregate = new DashboardPeriodAggregate();
        aggregate.setRechargeOrderCount(2L);
        aggregate.setRechargeReceiptCent(receipt);
        aggregate.setRechargePoints(receipt / 100);
        aggregate.setRefundOrderCount(1L);
        aggregate.setRefundOutflowCent(refunds);
        aggregate.setRefundPoints(refunds / 100);
        aggregate.setConsumptionOrderCount(3L);
        aggregate.setConsumptionGrossCent(consumption);
        aggregate.setConsumptionPoints(consumption / 100);
        aggregate.setPlatformFeeCent(consumption / 20);
        aggregate.setStorePayableCent(consumption - consumption / 20);
        return aggregate;
    }

    private DashboardDailyAggregate dailyAggregate(LocalDate date) {
        DashboardDailyAggregate aggregate = new DashboardDailyAggregate();
        aggregate.setBusinessDate(date);
        aggregate.setRechargeReceiptCent(10_000L);
        aggregate.setRefundOutflowCent(2_000L);
        aggregate.setConsumptionGrossCent(5_000L);
        aggregate.setPlatformFeeCent(250L);
        aggregate.setStorePayableCent(4_750L);
        return aggregate;
    }

    private DashboardBacklogAggregate consumptionBacklog() {
        DashboardBacklogAggregate aggregate = new DashboardBacklogAggregate();
        aggregate.setPendingConsumptionCount(2L);
        aggregate.setNotIncludedConsumptionCount(3L);
        aggregate.setNotIncludedPayableCent(28_500L);
        return aggregate;
    }

    private DashboardBacklogAggregate settlementBacklog() {
        DashboardBacklogAggregate aggregate = new DashboardBacklogAggregate();
        aggregate.setAwaitingStoreConfirmationCount(1L);
        aggregate.setAwaitingStoreConfirmationCent(9_500L);
        aggregate.setAwaitingPlatformPaymentCount(2L);
        aggregate.setAwaitingPlatformPaymentCent(57_000L);
        return aggregate;
    }

    private void allowSuperAdmin() {
        when(sysUserMapper.selectById(OPERATOR_ID)).thenReturn(activeOperator());
        when(staffAuthorityMapper.selectRoleCodes(OPERATOR_ID)).thenReturn(List.of("SUPER_ADMIN"));
    }

    private void allowStoreManager(List<Long> storeIds) {
        when(sysUserMapper.selectById(OPERATOR_ID)).thenReturn(activeOperator());
        when(staffAuthorityMapper.selectRoleCodes(OPERATOR_ID)).thenReturn(List.of("STORE_MANAGER"));
        when(staffAuthorityMapper.selectStoreIds(OPERATOR_ID)).thenReturn(storeIds);
    }

    private SysUser activeOperator() {
        return SysUser.builder().id(OPERATOR_ID).status(SysUserStatus.ACTIVE).build();
    }
}
