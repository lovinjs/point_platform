package com.core.coreboot.platform.consumption.model;

public record ConsumptionReversalCommand(
        String orderNo,
        Long operatorId,
        String reason,
        String idempotencyKey,
        String clientIp
) {
}
