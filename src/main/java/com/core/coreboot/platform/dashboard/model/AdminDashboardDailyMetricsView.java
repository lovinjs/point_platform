package com.core.coreboot.platform.dashboard.model;

import java.time.LocalDate;

public record AdminDashboardDailyMetricsView(
        LocalDate businessDate,
        long rechargeReceiptCent,
        long refundOutflowCent,
        long netRechargeCashCent,
        long consumptionGrossCent,
        long platformFeeCent,
        long storePayableCent
) {
}
