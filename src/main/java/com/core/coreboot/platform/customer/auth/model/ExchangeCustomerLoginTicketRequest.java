package com.core.coreboot.platform.customer.auth.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ExchangeCustomerLoginTicketRequest(
        @NotBlank(message = "登录票据不能为空")
        @Pattern(regexp = "[A-Za-z0-9_-]{43}", message = "登录票据格式错误")
        String ticket
) {
}
