package com.core.coreboot.platform.customer.model;

import com.core.coreboot.platform.common.enums.CustomerStatus;

import java.time.LocalDateTime;

public record AdminCustomerManagementView(
        Long customerId,
        String phone,
        String nickname,
        String avatarUrl,
        CustomerStatus status,
        long availablePoints,
        boolean consumePinConfigured,
        boolean consumePinLocked,
        LocalDateTime consumePinLockedUntil,
        LocalDateTime lastLoginTime,
        LocalDateTime createTime,
        LocalDateTime updateTime
) {
}
