package com.core.coreboot.platform.finance.model;

public record AdminFinancialSummaryView(
        long rechargeReceiptCent,
        long rechargePoints,
        long refundOutflowCent,
        long refundPoints,
        long netRechargeCashCent,
        long consumptionGrossCent,
        long consumptionPoints,
        long platformFeeCent,
        long storePayableCent,
        long notIncludedPayableCent,
        long includedPayableCent,
        long settledPayableCent,
        long settlementPaidCent
) {
}
