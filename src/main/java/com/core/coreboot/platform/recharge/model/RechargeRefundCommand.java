package com.core.coreboot.platform.recharge.model;

import com.core.coreboot.platform.common.enums.RefundMethod;

public record RechargeRefundCommand(
        String orderNo,
        Long operatorId,
        RefundMethod refundMethod,
        String refundReference,
        String reason,
        String idempotencyKey,
        String clientIp
) {
}
