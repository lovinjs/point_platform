package com.core.coreboot.platform.staff.model;

public record AdminStaffPasswordResetCommand(
        Long userId,
        String newPassword,
        Long operatorId,
        String clientIp
) {
}
