package com.core.coreboot.platform.setting.model;

public record CurrentBusinessPolicy(
        int platformFeeRateBps,
        int consumptionPendingTtlMinutes
) {
}
