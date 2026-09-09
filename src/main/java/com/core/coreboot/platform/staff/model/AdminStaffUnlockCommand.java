package com.core.coreboot.platform.staff.model;

public record AdminStaffUnlockCommand(
        Long userId,
        Long operatorId,
        String clientIp
) {
}
