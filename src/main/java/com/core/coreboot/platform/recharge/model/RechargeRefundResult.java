package com.core.coreboot.platform.recharge.model;

import com.core.coreboot.platform.common.enums.RechargeRefundStatus;
import com.core.coreboot.platform.common.enums.RefundMethod;

import java.time.LocalDateTime;

public record RechargeRefundResult(
        String refundNo,
        String rechargeOrderNo,
        Long customerId,
        long refundPoints,
        long refundAmountCent,
        RefundMethod refundMethod,
        RechargeRefundStatus refundStatus,
        long availablePoints,
        LocalDateTime completedTime
) {
}
