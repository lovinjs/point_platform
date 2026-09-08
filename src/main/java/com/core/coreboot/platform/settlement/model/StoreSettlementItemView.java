package com.core.coreboot.platform.settlement.model;

import com.core.coreboot.platform.common.enums.SettlementItemType;

import java.time.LocalDateTime;

public record StoreSettlementItemView(
        Long itemId,
        SettlementItemType itemType,
        String consumptionOrderNo,
        long pointsDelta,
        long grossAmountCent,
        long platformFeeCent,
        long storePayableCent,
        String adjustmentReason,
        LocalDateTime consumptionCompletedTime,
        LocalDateTime createTime
) {
}
