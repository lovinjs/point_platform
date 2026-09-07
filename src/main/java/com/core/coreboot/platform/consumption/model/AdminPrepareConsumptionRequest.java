package com.core.coreboot.platform.consumption.model;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record AdminPrepareConsumptionRequest(
        @NotNull(message = "客户不能为空")
        @Positive(message = "客户编号必须大于0")
        Long customerId,

        @NotNull(message = "消费门店不能为空")
        @Positive(message = "门店编号必须大于0")
        Long storeId,

        @NotNull(message = "消费积分不能为空")
        @Positive(message = "消费积分必须大于0")
        Long consumePoints,

        @Size(max = 500, message = "备注不能超过500位")
        String remark
) {
    public PrepareConsumptionCommand toCommand(
            Long operatorId,
            String idempotencyKey,
            String clientIp
    ) {
        return new PrepareConsumptionCommand(
                customerId,
                storeId,
                operatorId,
                consumePoints,
                idempotencyKey,
                remark,
                clientIp
        );
    }
}
