package com.core.coreboot.platform.consumption.model;

public record PrepareConsumptionCommand(
        Long customerId,
        Long storeId,
        Long operatorId,
        Long consumePoints,
        String idempotencyKey,
        String remark,
        String clientIp
) {
}
