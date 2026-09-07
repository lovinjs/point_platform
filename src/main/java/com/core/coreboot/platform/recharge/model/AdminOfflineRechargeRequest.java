package com.core.coreboot.platform.recharge.model;

import com.core.coreboot.exception.CustomException;
import com.core.coreboot.exception.ExceptionEnum;
import com.core.coreboot.platform.common.enums.PaymentMethod;
import com.core.coreboot.platform.common.model.PointMoneyPolicy;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record AdminOfflineRechargeRequest(
        @NotNull(message = "客户不能为空")
        @Positive(message = "客户编号必须大于0")
        Long customerId,

        @NotNull(message = "充值门店不能为空")
        @Positive(message = "门店编号必须大于0")
        Long storeId,

        @NotNull(message = "充值金额不能为空")
        @Positive(message = "充值金额必须大于0")
        Long amountYuan,

        @NotNull(message = "支付方式不能为空")
        PaymentMethod paymentMethod,

        @NotBlank(message = "平台收款交易参考号不能为空")
        @Size(max = 128, message = "平台收款交易参考号不能超过128位")
        String paymentReference,

        @Size(max = 500, message = "备注不能超过500位")
        String remark
) {
    public OfflineRechargeCommand toCommand(
            Long operatorId,
            String idempotencyKey,
            String clientIp
    ) {
        long amountCent;
        try {
            amountCent = Math.multiplyExact(amountYuan, PointMoneyPolicy.CENTS_PER_POINT);
        } catch (ArithmeticException ex) {
            throw new CustomException(ExceptionEnum.PLATFORM_RECHARGE_AMOUNT_INVALID);
        }
        return new OfflineRechargeCommand(
                customerId,
                storeId,
                operatorId,
                amountCent,
                paymentMethod,
                paymentReference,
                idempotencyKey,
                remark,
                clientIp
        );
    }
}
