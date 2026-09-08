package com.core.coreboot.platform.consumption.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ConfirmConsumptionRequest(
        @Schema(description = "当前客户的6位消费密码", format = "password", example = "258369")
        @NotBlank(message = "消费密码不能为空")
        @Pattern(regexp = "^\\d{6}$", message = "消费密码必须为6位数字")
        String consumePin
) {
}
