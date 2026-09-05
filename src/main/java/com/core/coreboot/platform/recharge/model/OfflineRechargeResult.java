package com.core.coreboot.platform.recharge.model;

import com.core.coreboot.platform.common.enums.RechargeOrderStatus;

public record OfflineRechargeResult(
        String orderNo,
        Long customerId,
        Long storeId,
        long amountCent,
        long rechargePoints,
        RechargeOrderStatus orderStatus
) {
}
