package com.core.coreboot.platform.dashboard.service.impl;

import com.core.coreboot.exception.CustomException;
import com.core.coreboot.exception.ExceptionEnum;
import com.core.coreboot.platform.common.enums.RoleCode;
import com.core.coreboot.platform.common.enums.SysUserStatus;
import com.core.coreboot.platform.dashboard.mapper.DashboardOverviewMapper;
import com.core.coreboot.platform.dashboard.model.AdminDashboardBacklogView;
import com.core.coreboot.platform.dashboard.model.AdminDashboardDailyMetricsView;
import com.core.coreboot.platform.dashboard.model.AdminDashboardOverviewView;
import com.core.coreboot.platform.dashboard.model.AdminDashboardPeriodMetricsView;
import com.core.coreboot.platform.dashboard.model.DashboardBacklogAggregate;
import com.core.coreboot.platform.dashboard.model.DashboardDailyAggregate;
import com.core.coreboot.platform.dashboard.model.DashboardPeriodAggregate;
import com.core.coreboot.platform.dashboard.service.AdminDashboardService;
import com.core.coreboot.platform.merchant.entity.Store;
import com.core.coreboot.platform.merchant.mapper.StoreMapper;
import com.core.coreboot.platform.staff.entity.SysUser;
import com.core.coreboot.platform.staff.mapper.StaffAuthorityMapper;
import com.core.coreboot.platform.staff.mapper.SysUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@Transactional(readOnly = true)
public class AdminDashboardServiceImpl implements AdminDashboardService {
    private static final List<Integer> SUPPORTED_TREND_DAYS = List.of(7, 30);

    private final DashboardOverviewMapper dashboardMapper;
    private final StoreMapper storeMapper;
    private final SysUserMapper sysUserMapper;
    private final StaffAuthorityMapper staffAuthorityMapper;
    private final Clock clock;

    @Autowired
    public AdminDashboardServiceImpl(
            DashboardOverviewMapper dashboardMapper,
            StoreMapper storeMapper,
            SysUserMapper sysUserMapper,
            StaffAuthorityMapper staffAuthorityMapper
    ) {
        this(
                dashboardMapper,
                storeMapper,
                sysUserMapper,
                staffAuthorityMapper,
                Clock.systemDefaultZone()
        );
    }

    AdminDashboardServiceImpl(
            DashboardOverviewMapper dashboardMapper,
            StoreMapper storeMapper,
            SysUserMapper sysUserMapper,
            StaffAuthorityMapper staffAuthorityMapper,
            Clock clock
    ) {
        this.dashboardMapper = dashboardMapper;
        this.storeMapper = storeMapper;
        this.sysUserMapper = sysUserMapper;
        this.staffAuthorityMapper = staffAuthorityMapper;
        this.clock = clock;
    }

    @Override
    public AdminDashboardOverviewView overview(Long operatorId, Long storeId, int trendDays) {
        OperatorScope scope = requireScope(operatorId);
        if (!SUPPORTED_TREND_DAYS.contains(trendDays)) {
            throw new CustomException(ExceptionEnum.PLATFORM_INVALID_REQUEST);
        }
        Store selectedStore = requireSelectedStore(scope, storeId);
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDate today = now.toLocalDate();
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate trendStart = today.minusDays(trendDays - 1L);
        LocalDateTime endTimeExclusive = today.plusDays(1).atStartOfDay();

        QueryScope queryScope = queryScope(scope, storeId);
        AdminDashboardPeriodMetricsView todayMetrics;
        AdminDashboardPeriodMetricsView monthMetrics;
        AdminDashboardBacklogView backlog;
        List<AdminDashboardDailyMetricsView> trend;
        if (queryScope.empty()) {
            todayMetrics = emptyPeriodMetrics();
            monthMetrics = emptyPeriodMetrics();
            backlog = emptyBacklog();
            trend = emptyTrend(trendStart, today);
        } else {
            todayMetrics = toPeriodMetrics(dashboardMapper.selectPeriodMetrics(
                    today.atStartOfDay(), endTimeExclusive, queryScope.storeId(), queryScope.storeIds()
            ));
            monthMetrics = toPeriodMetrics(dashboardMapper.selectPeriodMetrics(
                    monthStart.atStartOfDay(), endTimeExclusive, queryScope.storeId(), queryScope.storeIds()
            ));
            backlog = loadBacklog(now, queryScope);
            trend = toTrend(
                    dashboardMapper.selectDailyMetrics(
                            trendStart.atStartOfDay(),
                            endTimeExclusive,
                            queryScope.storeId(),
                            queryScope.storeIds()
                    ),
                    trendStart,
                    today
            );
        }

        return new AdminDashboardOverviewView(
                now,
                today,
                monthStart,
                trendStart,
                trendDays,
                scope.unrestricted(),
                selectedStore == null ? null : selectedStore.getId(),
                selectedStore == null ? null : selectedStore.getStoreCode(),
                selectedStore == null ? null : selectedStore.getStoreName(),
                todayMetrics,
                monthMetrics,
                backlog,
                trend
        );
    }

    private AdminDashboardPeriodMetricsView toPeriodMetrics(DashboardPeriodAggregate aggregate) {
        if (aggregate == null) {
            return emptyPeriodMetrics();
        }
        long rechargeReceiptCent = nonNegative(aggregate.getRechargeReceiptCent());
        long refundOutflowCent = nonNegative(aggregate.getRefundOutflowCent());
        long consumptionGrossCent = nonNegative(aggregate.getConsumptionGrossCent());
        long platformFeeCent = nonNegative(aggregate.getPlatformFeeCent());
        long storePayableCent = nonNegative(aggregate.getStorePayableCent());
        requireMoneyBreakdown(consumptionGrossCent, platformFeeCent, storePayableCent);
        return new AdminDashboardPeriodMetricsView(
                nonNegative(aggregate.getRechargeOrderCount()),
                rechargeReceiptCent,
                nonNegative(aggregate.getRechargePoints()),
                nonNegative(aggregate.getRefundOrderCount()),
                refundOutflowCent,
                nonNegative(aggregate.getRefundPoints()),
                subtract(rechargeReceiptCent, refundOutflowCent),
                nonNegative(aggregate.getConsumptionOrderCount()),
                consumptionGrossCent,
                nonNegative(aggregate.getConsumptionPoints()),
                platformFeeCent,
                storePayableCent
        );
    }

    private AdminDashboardBacklogView loadBacklog(LocalDateTime now, QueryScope queryScope) {
        DashboardBacklogAggregate consumption = dashboardMapper.selectConsumptionBacklog(
                now, queryScope.storeId(), queryScope.storeIds()
        );
        DashboardBacklogAggregate settlement = dashboardMapper.selectSettlementBacklog(
                queryScope.storeId(), queryScope.storeIds()
        );
        return new AdminDashboardBacklogView(
                consumption == null ? 0 : nonNegative(consumption.getPendingConsumptionCount()),
                consumption == null ? 0 : nonNegative(consumption.getNotIncludedConsumptionCount()),
                consumption == null ? 0 : nonNegative(consumption.getNotIncludedPayableCent()),
                settlement == null ? 0 : nonNegative(settlement.getAwaitingStoreConfirmationCount()),
                settlement == null ? 0 : nonNegative(settlement.getAwaitingStoreConfirmationCent()),
                settlement == null ? 0 : nonNegative(settlement.getAwaitingPlatformPaymentCount()),
                settlement == null ? 0 : nonNegative(settlement.getAwaitingPlatformPaymentCent())
        );
    }

    private List<AdminDashboardDailyMetricsView> toTrend(
            List<DashboardDailyAggregate> rows,
            LocalDate startDate,
            LocalDate endDate
    ) {
        Map<LocalDate, DashboardDailyAggregate> byDate = new LinkedHashMap<>();
        if (rows != null) {
            for (DashboardDailyAggregate row : rows) {
                if (row == null || row.getBusinessDate() == null
                        || row.getBusinessDate().isBefore(startDate)
                        || row.getBusinessDate().isAfter(endDate)
                        || byDate.putIfAbsent(row.getBusinessDate(), row) != null) {
                    throw new CustomException(ExceptionEnum.PLATFORM_DATA_CONFLICT);
                }
            }
        }
        return startDate.datesUntil(endDate.plusDays(1))
                .map(date -> toDailyMetrics(date, byDate.get(date)))
                .toList();
    }

    private AdminDashboardDailyMetricsView toDailyMetrics(
            LocalDate date,
            DashboardDailyAggregate aggregate
    ) {
        if (aggregate == null) {
            return new AdminDashboardDailyMetricsView(date, 0, 0, 0, 0, 0, 0);
        }
        long rechargeReceiptCent = nonNegative(aggregate.getRechargeReceiptCent());
        long refundOutflowCent = nonNegative(aggregate.getRefundOutflowCent());
        long consumptionGrossCent = nonNegative(aggregate.getConsumptionGrossCent());
        long platformFeeCent = nonNegative(aggregate.getPlatformFeeCent());
        long storePayableCent = nonNegative(aggregate.getStorePayableCent());
        requireMoneyBreakdown(consumptionGrossCent, platformFeeCent, storePayableCent);
        return new AdminDashboardDailyMetricsView(
                date,
                rechargeReceiptCent,
                refundOutflowCent,
                subtract(rechargeReceiptCent, refundOutflowCent),
                consumptionGrossCent,
                platformFeeCent,
                storePayableCent
        );
    }

    private Store requireSelectedStore(OperatorScope scope, Long storeId) {
        if (storeId == null) {
            return null;
        }
        if (storeId <= 0) {
            throw new CustomException(ExceptionEnum.PLATFORM_INVALID_REQUEST);
        }
        if (!scope.unrestricted() && !scope.storeIds().contains(storeId)) {
            throw new CustomException(ExceptionEnum.PLATFORM_STORE_ACCESS_DENIED);
        }
        Store store = storeMapper.selectById(storeId);
        if (store == null) {
            throw new CustomException(ExceptionEnum.PLATFORM_STORE_NOT_FOUND);
        }
        return store;
    }

    private OperatorScope requireScope(Long operatorId) {
        if (operatorId == null || operatorId <= 0) {
            throw new CustomException(ExceptionEnum.PLATFORM_ADMIN_TOKEN_INVALID);
        }
        SysUser operator = sysUserMapper.selectById(operatorId);
        if (operator == null) {
            throw new CustomException(ExceptionEnum.PLATFORM_OPERATOR_NOT_FOUND);
        }
        if (operator.getStatus() != SysUserStatus.ACTIVE) {
            throw new CustomException(ExceptionEnum.PLATFORM_OPERATOR_DISABLED);
        }
        List<String> roles = staffAuthorityMapper.selectRoleCodes(operatorId);
        if (roles != null && roles.contains(RoleCode.SUPER_ADMIN.getCode())) {
            return new OperatorScope(true, List.of());
        }
        if (roles == null || !roles.contains(RoleCode.STORE_MANAGER.getCode())) {
            throw new CustomException(ExceptionEnum.PLATFORM_ADMIN_ACCESS_DENIED);
        }
        List<Long> storeIds = staffAuthorityMapper.selectStoreIds(operatorId);
        if (storeIds == null) {
            storeIds = List.of();
        }
        return new OperatorScope(false, storeIds.stream()
                .filter(Objects::nonNull)
                .filter(id -> id > 0)
                .distinct()
                .toList());
    }

    private QueryScope queryScope(OperatorScope scope, Long selectedStoreId) {
        if (selectedStoreId != null) {
            return new QueryScope(false, selectedStoreId, null);
        }
        if (scope.unrestricted()) {
            return new QueryScope(false, null, null);
        }
        if (scope.storeIds().isEmpty()) {
            return new QueryScope(true, null, List.of());
        }
        return new QueryScope(false, null, scope.storeIds());
    }

    private AdminDashboardPeriodMetricsView emptyPeriodMetrics() {
        return new AdminDashboardPeriodMetricsView(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    }

    private AdminDashboardBacklogView emptyBacklog() {
        return new AdminDashboardBacklogView(0, 0, 0, 0, 0, 0, 0);
    }

    private List<AdminDashboardDailyMetricsView> emptyTrend(LocalDate startDate, LocalDate endDate) {
        return startDate.datesUntil(endDate.plusDays(1))
                .map(date -> new AdminDashboardDailyMetricsView(date, 0, 0, 0, 0, 0, 0))
                .toList();
    }

    private long nonNegative(Long value) {
        if (value == null) {
            return 0L;
        }
        if (value < 0) {
            throw new CustomException(ExceptionEnum.PLATFORM_DATA_CONFLICT);
        }
        return value;
    }

    private long subtract(long left, long right) {
        try {
            return Math.subtractExact(left, right);
        } catch (ArithmeticException ex) {
            throw new CustomException(ExceptionEnum.PLATFORM_DATA_CONFLICT);
        }
    }

    private void requireMoneyBreakdown(long gross, long fee, long payable) {
        try {
            if (Math.addExact(fee, payable) != gross) {
                throw new CustomException(ExceptionEnum.PLATFORM_DATA_CONFLICT);
            }
        } catch (ArithmeticException ex) {
            throw new CustomException(ExceptionEnum.PLATFORM_DATA_CONFLICT);
        }
    }

    private record OperatorScope(boolean unrestricted, List<Long> storeIds) {
    }

    private record QueryScope(boolean empty, Long storeId, List<Long> storeIds) {
    }
}
