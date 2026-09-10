package com.core.coreboot.platform.customer.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ConsumePinResetVerificationRequest(
        @Schema(description = "绑定手机号收到的6位验证码", example = "825391")
        @NotBlank(message = "验证码不能为空")
        @Pattern(regexp = "^\\d{6}$", message = "验证码必须为6位数字")
        String verificationCode
) {
}
