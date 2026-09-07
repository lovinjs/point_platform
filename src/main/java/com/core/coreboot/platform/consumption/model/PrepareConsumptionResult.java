package com.core.coreboot.platform.consumption.model;

import com.core.coreboot.platform.common.enums.ConsumptionOrderStatus;
import com.core.coreboot.platform.common.enums.ConsumptionVerificationMode;

import java.time.LocalDateTime;

public record PrepareConsumptionResult(
        String orderNo,
        Long customerId,
        Long storeId,
        long consumePoints,
        long grossAmountCent,
        int platformFeeRateBps,
        long platformFeeCent,
        long storePayableCent,
        ConsumptionVerificationMode verificationMode,
        LocalDateTime expiresTime,
        ConsumptionOrderStatus orderStatus
) {
}
