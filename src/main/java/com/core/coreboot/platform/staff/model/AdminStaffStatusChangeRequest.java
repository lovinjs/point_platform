package com.core.coreboot.platform.staff.model;

import com.core.coreboot.platform.common.enums.SysUserStatus;
import jakarta.validation.constraints.NotNull;

public record AdminStaffStatusChangeRequest(
        @NotNull(message = "目标状态不能为空")
        SysUserStatus status
) {
    public AdminStaffStatusChangeCommand toCommand(Long userId, Long operatorId, String clientIp) {
        return new AdminStaffStatusChangeCommand(userId, status, operatorId, clientIp);
    }
}
