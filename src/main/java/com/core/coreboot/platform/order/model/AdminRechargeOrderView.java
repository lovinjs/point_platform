package com.core.coreboot.platform.order.model;

import com.core.coreboot.platform.common.enums.FundReceiver;
import com.core.coreboot.platform.common.enums.PaymentMethod;
import com.core.coreboot.platform.common.enums.RechargeChannel;
import com.core.coreboot.platform.common.enums.RechargeOrderStatus;
import com.core.coreboot.platform.common.enums.RechargeRefundStatus;
import com.core.coreboot.platform.common.enums.RefundMethod;

import java.time.LocalDateTime;

public record AdminRechargeOrderView(
        String orderNo,
        Long customerId,
        String customerPhone,
        String customerNickname,
        Long storeId,
        String storeCode,
        String storeName,
        long rechargePoints,
        long amountCent,
        RechargeChannel channel,
        PaymentMethod paymentMethod,
        FundReceiver fundReceiver,
        String paymentReference,
        RechargeOrderStatus orderStatus,
        Long operatorId,
        String operatorName,
        String remark,
        LocalDateTime paidTime,
        LocalDateTime completedTime,
        LocalDateTime createTime,
        String refundNo,
        RefundMethod refundMethod,
        String refundReference,
        RechargeRefundStatus refundStatus,
        String refundReason,
        LocalDateTime refundCompletedTime
) {
}
