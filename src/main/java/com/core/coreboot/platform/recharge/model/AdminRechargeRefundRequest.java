package com.core.coreboot.platform.recharge.model;

import com.core.coreboot.platform.common.enums.RefundMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminRechargeRefundRequest(
        @NotNull(message = "退款方式不能为空")
        RefundMethod refundMethod,

        @NotBlank(message = "实际退款交易参考号不能为空")
        @Size(max = 128, message = "实际退款交易参考号不能超过128位")
        String refundReference,

        @NotBlank(message = "退款原因不能为空")
        @Size(max = 500, message = "退款原因不能超过500位")
        String reason
) {
    public RechargeRefundCommand toCommand(
            String orderNo,
            Long operatorId,
            String idempotencyKey,
            String clientIp
    ) {
        return new RechargeRefundCommand(
                orderNo,
                operatorId,
                refundMethod,
                refundReference,
                reason,
                idempotencyKey,
                clientIp
        );
    }
}
