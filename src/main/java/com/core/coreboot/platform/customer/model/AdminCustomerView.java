package com.core.coreboot.platform.customer.model;

import com.core.coreboot.platform.common.enums.CustomerStatus;

public record AdminCustomerView(
        Long customerId,
        String phone,
        String nickname,
        CustomerStatus status,
        boolean consumePinConfigured,
        long availablePoints
) {
}
