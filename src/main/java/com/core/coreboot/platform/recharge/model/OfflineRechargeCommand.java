package com.core.coreboot.platform.recharge.model;

import com.core.coreboot.platform.common.enums.PaymentMethod;

public record OfflineRechargeCommand(
        Long customerId,
        Long storeId,
        Long operatorId,
        Long amountCent,
        PaymentMethod paymentMethod,
        String paymentReference,
        String idempotencyKey,
        String remark
) {
}
