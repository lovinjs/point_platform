package com.core.coreboot.platform.auth.service;

import com.core.coreboot.platform.common.enums.RoleCode;

import java.util.Set;

public interface AdminPasswordService {
    void changeOwnPassword(
            Long userId,
            Set<RoleCode> roles,
            String currentPassword,
            String newPassword,
            String clientIp
    );
}
