package com.core.coreboot.platform.dashboard.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record AdminDashboardOverviewView(
        LocalDateTime generatedTime,
        LocalDate todayDate,
        LocalDate currentMonthStartDate,
        LocalDate trendStartDate,
        int trendDays,
        boolean globalScope,
        Long selectedStoreId,
        String selectedStoreCode,
        String selectedStoreName,
        AdminDashboardPeriodMetricsView today,
        AdminDashboardPeriodMetricsView currentMonth,
        AdminDashboardBacklogView backlog,
        List<AdminDashboardDailyMetricsView> trend
) {
    public AdminDashboardOverviewView {
        trend = trend == null ? List.of() : List.copyOf(trend);
    }
}
