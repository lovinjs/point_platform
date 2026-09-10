package com.core.coreboot.platform.customer.model;

import com.core.coreboot.platform.common.enums.CustomerStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminCustomerStatusChangeRequest(
        @NotNull(message = "目标状态不能为空")
        CustomerStatus status,
        @NotBlank(message = "操作原因不能为空")
        @Size(max = 500, message = "操作原因不能超过500个字符")
        String reason
) {
    public AdminCustomerStatusChangeCommand toCommand(
            Long customerId,
            Long operatorId,
            String clientIp
    ) {
        return new AdminCustomerStatusChangeCommand(
                customerId,
                status,
                reason,
                operatorId,
                clientIp
        );
    }
}
