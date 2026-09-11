package com.core.coreboot.platform.setting.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PlatformBusinessSettingUpdateRequest(
        @NotNull(message = "平台手续费率不能为空")
        @Min(value = 0, message = "平台手续费率不能小于0")
        @Max(value = 10000, message = "平台手续费率不能超过100%")
        Integer platformFeeRateBps,

        @NotNull(message = "消费确认有效期不能为空")
        @Min(value = 1, message = "消费确认有效期不能少于1分钟")
        @Max(value = 30, message = "消费确认有效期不能超过30分钟")
        Integer consumptionPendingTtlMinutes,

        @NotNull(message = "配置版本不能为空")
        @Min(value = 0, message = "配置版本不正确")
        Long version,

        @NotBlank(message = "修改原因不能为空")
        @Size(max = 500, message = "修改原因不能超过500个字符")
        String changeReason
) {
    public PlatformBusinessSettingUpdateCommand toCommand(Long operatorId, String clientIp) {
        return new PlatformBusinessSettingUpdateCommand(
                platformFeeRateBps,
                consumptionPendingTtlMinutes,
                version,
                changeReason,
                operatorId,
                clientIp
        );
    }
}
