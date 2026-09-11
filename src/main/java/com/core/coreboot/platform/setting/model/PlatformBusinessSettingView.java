package com.core.coreboot.platform.setting.model;

import java.time.LocalDateTime;

public record PlatformBusinessSettingView(
        int pointsPerYuan,
        boolean bonusPointsEnabled,
        int platformFeeRateBps,
        int consumptionPendingTtlMinutes,
        long version,
        Long lastUpdatedBy,
        String lastUpdatedByName,
        LocalDateTime updateTime
) {
}
