package com.core.coreboot.platform.customer.model;

import com.core.coreboot.platform.common.enums.CustomerStatus;

public record AdminCustomerStatusChangeCommand(
        Long customerId,
        CustomerStatus status,
        String reason,
        Long operatorId,
        String clientIp
) {
}
