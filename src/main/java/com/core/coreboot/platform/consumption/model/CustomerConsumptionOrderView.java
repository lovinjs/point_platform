package com.core.coreboot.platform.consumption.model;

import com.core.coreboot.platform.common.enums.ConsumptionOrderStatus;

import java.time.LocalDateTime;

public record CustomerConsumptionOrderView(
        String orderNo,
        Long storeId,
        String storeName,
        long consumePoints,
        long amountCent,
        ConsumptionOrderStatus orderStatus,
        String remark,
        LocalDateTime expiresTime,
        LocalDateTime confirmedTime,
        LocalDateTime completedTime,
        LocalDateTime createTime
) {
}
