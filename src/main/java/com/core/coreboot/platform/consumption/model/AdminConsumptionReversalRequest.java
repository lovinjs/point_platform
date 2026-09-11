package com.core.coreboot.platform.consumption.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminConsumptionReversalRequest(
        @NotBlank(message = "冲正原因不能为空")
        @Size(max = 500, message = "冲正原因不能超过500位")
        String reason
) {
    public ConsumptionReversalCommand toCommand(
            String orderNo,
            Long operatorId,
            String idempotencyKey,
            String clientIp
    ) {
        return new ConsumptionReversalCommand(
                orderNo,
                operatorId,
                reason,
                idempotencyKey,
                clientIp
        );
    }
}
