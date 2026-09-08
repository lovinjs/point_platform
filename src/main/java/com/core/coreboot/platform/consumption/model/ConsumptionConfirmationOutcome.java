package com.core.coreboot.platform.consumption.model;

public record ConsumptionConfirmationOutcome(
        boolean expired,
        ConsumptionConfirmationResult result
) {
    public static ConsumptionConfirmationOutcome expiredOrder() {
        return new ConsumptionConfirmationOutcome(true, null);
    }

    public static ConsumptionConfirmationOutcome completed(ConsumptionConfirmationResult result) {
        return new ConsumptionConfirmationOutcome(false, result);
    }
}
