package com.core.coreboot.platform.auth.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminChangePasswordRequest(
        @Schema(description = "当前登录密码", format = "password")
        @NotBlank(message = "当前登录密码不能为空")
        @Size(max = 128, message = "当前登录密码长度不能超过128位")
        String currentPassword,

        @Schema(description = "新的强密码", format = "password", example = "NewPassword!123")
        @NotBlank(message = "新登录密码不能为空")
        @Size(min = 12, max = 128, message = "新登录密码长度必须为12到128位")
        String newPassword
) {
}
