package com.core.coreboot.platform.staff.model;

import com.core.coreboot.platform.common.enums.RoleCode;

public record AdminStaffUpdateCommand(
        Long userId,
        String realName,
        String phone,
        RoleCode roleCode,
        Long storeId,
        Long operatorId,
        String clientIp
) {
}
