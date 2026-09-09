package com.core.coreboot.platform.staff.model;

import com.core.coreboot.platform.common.enums.RoleCode;
import com.core.coreboot.platform.common.enums.SysUserStatus;

import java.time.LocalDateTime;
import java.util.List;

public record AdminStaffManagementView(
        Long userId,
        String username,
        String phone,
        String realName,
        SysUserStatus status,
        List<RoleCode> roleCodes,
        List<AdminStaffStoreView> stores,
        int failedLoginCount,
        LocalDateTime lockedUntil,
        boolean loginLocked,
        LocalDateTime lastLoginTime,
        LocalDateTime passwordUpdatedTime,
        LocalDateTime createTime,
        LocalDateTime updateTime
) {
    public AdminStaffManagementView {
        roleCodes = roleCodes == null ? List.of() : List.copyOf(roleCodes);
        stores = stores == null ? List.of() : List.copyOf(stores);
    }
}
