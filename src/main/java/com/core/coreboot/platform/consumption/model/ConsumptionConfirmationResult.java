package com.core.coreboot.platform.consumption.model;

import com.core.coreboot.platform.common.enums.ConsumptionOrderStatus;

import java.time.LocalDateTime;

public record ConsumptionConfirmationResult(
        String orderNo,
        Long storeId,
        String storeName,
        long consumePoints,
        long amountCent,
        long availablePoints,
        ConsumptionOrderStatus orderStatus,
        LocalDateTime completedTime
) {
}
