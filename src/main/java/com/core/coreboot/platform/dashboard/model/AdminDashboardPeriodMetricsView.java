package com.core.coreboot.platform.dashboard.model;

public record AdminDashboardPeriodMetricsView(
        long rechargeOrderCount,
        long rechargeReceiptCent,
        long rechargePoints,
        long refundOrderCount,
        long refundOutflowCent,
        long refundPoints,
        long netRechargeCashCent,
        long consumptionOrderCount,
        long consumptionGrossCent,
        long consumptionPoints,
        long platformFeeCent,
        long storePayableCent
) {
}
