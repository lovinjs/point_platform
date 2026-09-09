package com.core.coreboot.platform.consumption.model;

import com.core.coreboot.platform.common.enums.ConsumptionOrderStatus;
import com.core.coreboot.platform.common.enums.ConsumptionVerificationMode;

import java.time.LocalDateTime;

public record AdminConsumptionOrderStatusView(
        String orderNo,
        Long customerId,
        Long storeId,
        long consumePoints,
        long amountCent,
        ConsumptionVerificationMode verificationMode,
        ConsumptionOrderStatus orderStatus,
        String remark,
        LocalDateTime expiresTime,
        LocalDateTime confirmedTime,
        LocalDateTime completedTime,
        LocalDateTime createTime
) {
}
