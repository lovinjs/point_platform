package com.core.coreboot.platform.staff.model;

import com.core.coreboot.platform.common.enums.SysUserStatus;

public record AdminStaffStatusChangeCommand(
        Long userId,
        SysUserStatus status,
        Long operatorId,
        String clientIp
) {
}
