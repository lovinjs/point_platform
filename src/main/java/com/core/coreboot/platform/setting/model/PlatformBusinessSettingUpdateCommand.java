package com.core.coreboot.platform.setting.model;

public record PlatformBusinessSettingUpdateCommand(
        int platformFeeRateBps,
        int consumptionPendingTtlMinutes,
        long version,
        String changeReason,
        Long operatorId,
        String clientIp
) {
}
