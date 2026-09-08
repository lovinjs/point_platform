package com.core.coreboot.platform.settlement.model;

import com.core.coreboot.platform.common.enums.StoreSettlementStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record StoreSettlementSummaryView(
        String settlementNo,
        String periodCode,
        LocalDate startDate,
        LocalDate endDate,
        Long merchantId,
        Long storeId,
        String storeName,
        long totalConsumePoints,
        long grossAmountCent,
        long platformFeeCent,
        long adjustmentAmountCent,
        long payableAmountCent,
        StoreSettlementStatus settlementStatus,
        LocalDateTime confirmedTime,
        LocalDateTime paidTime,
        String paymentReference,
        String remark,
        LocalDateTime createTime,
        LocalDateTime updateTime
) {
}
