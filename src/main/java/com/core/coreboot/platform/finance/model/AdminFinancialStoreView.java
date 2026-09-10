package com.core.coreboot.platform.finance.model;

import com.core.coreboot.platform.common.enums.StoreStatus;

public record AdminFinancialStoreView(
        Long storeId,
        String storeCode,
        String storeName,
        StoreStatus storeStatus,
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
