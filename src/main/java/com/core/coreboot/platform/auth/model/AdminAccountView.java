package com.core.coreboot.platform.auth.model;

import com.core.coreboot.platform.auth.security.AdminUserPrincipal;
import com.core.coreboot.platform.common.enums.RoleCode;

import java.util.Comparator;
import java.util.List;

public record AdminAccountView(
        Long userId,
        String username,
        String realName,
        List<RoleCode> roles,
        List<Long> storeIds
) {
    public static AdminAccountView from(AdminUserPrincipal principal) {
        List<RoleCode> roles = principal.getRoles().stream()
                .sorted(Comparator.comparing(RoleCode::getCode))
                .toList();
        List<Long> storeIds = principal.getStoreIds().stream().sorted().toList();
        return new AdminAccountView(
                principal.getUserId(),
                principal.getUsername(),
                principal.getRealName(),
                roles,
                storeIds
        );
    }
}
