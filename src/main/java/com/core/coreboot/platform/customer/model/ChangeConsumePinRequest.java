package com.core.coreboot.platform.customer.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ChangeConsumePinRequest(
        @Schema(description = "当前6位消费密码", format = "password", example = "258369")
        @NotBlank(message = "当前消费密码不能为空")
        @Pattern(regexp = "^\\d{6}$", message = "当前消费密码必须为6位数字")
        String currentPin,

        @Schema(description = "新的6位消费密码", format = "password", example = "369258")
        @NotBlank(message = "新消费密码不能为空")
        @Pattern(regexp = "^\\d{6}$", message = "新消费密码必须为6位数字")
        String newPin
) {
}
