package com.core.coreboot.platform.staff.model;

import com.core.coreboot.platform.common.enums.RoleCode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record AdminStaffCreateRequest(
        @NotBlank(message = "用户名不能为空")
        @Size(min = 3, max = 64, message = "用户名长度必须为3到64位")
        @Pattern(regexp = "[A-Za-z0-9._-]+", message = "用户名只能包含字母、数字、点、下划线或横线")
        String username,

        @NotBlank(message = "初始密码不能为空")
        @Size(min = 12, max = 128, message = "初始密码长度必须为12到128位")
        String initialPassword,

        @NotBlank(message = "姓名不能为空")
        @Size(max = 64, message = "姓名不能超过64个字符")
        String realName,

        @Size(max = 32, message = "手机号不能超过32个字符")
        @Pattern(regexp = "^$|[0-9+()\\-\\s]{5,32}", message = "手机号格式不正确")
        String phone,

        @NotNull(message = "员工角色不能为空")
        RoleCode roleCode,

        @NotNull(message = "所属门店不能为空")
        @Positive(message = "所属门店不正确")
        Long storeId
) {
    public AdminStaffCreateCommand toCommand(Long operatorId, String clientIp) {
        return new AdminStaffCreateCommand(
                username,
                initialPassword,
                realName,
                phone,
                roleCode,
                storeId,
                operatorId,
                clientIp
        );
    }
}
