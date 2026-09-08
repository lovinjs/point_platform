package com.core.coreboot.platform.recharge.model;

import com.core.coreboot.platform.common.enums.PaymentMethod;
import com.core.coreboot.platform.common.enums.RechargeChannel;
import com.core.coreboot.platform.common.enums.RechargeOrderStatus;

import java.time.LocalDateTime;

public record CustomerRechargeOrderView(
        String orderNo,
        Long storeId,
        String storeName,
        long rechargePoints,
        long amountCent,
        RechargeChannel channel,
        PaymentMethod paymentMethod,
        RechargeOrderStatus orderStatus,
        String remark,
        LocalDateTime paidTime,
        LocalDateTime completedTime,
        LocalDateTime createTime
) {
}
