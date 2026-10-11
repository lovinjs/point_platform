package com.core.coreboot.platform.recharge.model;

public record RechargeRefundEligibility(
        String orderNo,
        boolean refundable,
        String unavailableReason
) {
}
