package com.core.coreboot.platform.staff.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminStaffPasswordResetRequest(
        @NotBlank(message = "新密码不能为空")
        @Size(min = 12, max = 128, message = "新密码长度必须为12到128位")
        String newPassword
) {
    public AdminStaffPasswordResetCommand toCommand(Long userId, Long operatorId, String clientIp) {
        return new AdminStaffPasswordResetCommand(userId, newPassword, operatorId, clientIp);
    }
}
