package com.core.coreboot.platform.consumption.model;

import com.core.coreboot.platform.common.enums.ConsumptionOrderStatus;

import java.time.LocalDateTime;

public record CustomerPendingConsumptionView(
        String orderNo,
        Long storeId,
        String storeName,
        long consumePoints,
        long amountCent,
        LocalDateTime expiresTime,
        ConsumptionOrderStatus orderStatus,
        String remark,
        LocalDateTime createTime
) {
}
