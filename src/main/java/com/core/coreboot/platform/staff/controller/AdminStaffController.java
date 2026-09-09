package com.core.coreboot.platform.staff.controller;

import com.core.coreboot.common.ApiRestResponse;
import com.core.coreboot.platform.auth.security.AdminUserPrincipal;
import com.core.coreboot.platform.common.enums.RoleCode;
import com.core.coreboot.platform.common.enums.SysUserStatus;
import com.core.coreboot.platform.common.model.PageResult;
import com.core.coreboot.platform.staff.model.AdminStaffCreateRequest;
import com.core.coreboot.platform.staff.model.AdminStaffManagementView;
import com.core.coreboot.platform.staff.model.AdminStaffPasswordResetRequest;
import com.core.coreboot.platform.staff.model.AdminStaffStatusChangeRequest;
import com.core.coreboot.platform.staff.model.AdminStaffStoreView;
import com.core.coreboot.platform.staff.model.AdminStaffUnlockCommand;
import com.core.coreboot.platform.staff.model.AdminStaffUpdateRequest;
import com.core.coreboot.platform.staff.service.AdminStaffManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "积分平台 - 后台员工账号")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/staff")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AdminStaffController {
    private final AdminStaffManagementService staffManagementService;

    @Operation(summary = "分页查询后台员工账号")
    @GetMapping
    public ApiRestResponse<PageResult<AdminStaffManagementView>> page(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) RoleCode roleCode,
            @RequestParam(required = false) SysUserStatus status,
            @RequestParam(required = false) Long storeId,
            @RequestParam(required = false) String keyword,
            @AuthenticationPrincipal AdminUserPrincipal principal
    ) {
        return ApiRestResponse.success(staffManagementService.list(
                principal.getUserId(), pageNum, pageSize, roleCode, status, storeId, keyword
        ));
    }

    @Operation(summary = "查询可分配给员工的门店")
    @GetMapping("/store-options")
    public ApiRestResponse<List<AdminStaffStoreView>> storeOptions(
            @AuthenticationPrincipal AdminUserPrincipal principal
    ) {
        return ApiRestResponse.success(staffManagementService.listAssignableStores(principal.getUserId()));
    }

    @Operation(summary = "创建店员或店长账号")
    @PostMapping
    public ApiRestResponse<AdminStaffManagementView> create(
            @Valid @RequestBody AdminStaffCreateRequest request,
            @AuthenticationPrincipal AdminUserPrincipal principal,
            HttpServletRequest servletRequest
    ) {
        return ApiRestResponse.success(staffManagementService.create(
                request.toCommand(principal.getUserId(), servletRequest.getRemoteAddr())
        ));
    }

    @Operation(summary = "修改员工资料、角色和所属门店")
    @PutMapping("/{userId}")
    public ApiRestResponse<AdminStaffManagementView> update(
            @PathVariable Long userId,
            @Valid @RequestBody AdminStaffUpdateRequest request,
            @AuthenticationPrincipal AdminUserPrincipal principal,
            HttpServletRequest servletRequest
    ) {
        return ApiRestResponse.success(staffManagementService.update(
                request.toCommand(userId, principal.getUserId(), servletRequest.getRemoteAddr())
        ));
    }

    @Operation(summary = "启用或停用员工账号")
    @PutMapping("/{userId}/status")
    public ApiRestResponse<AdminStaffManagementView> changeStatus(
            @PathVariable Long userId,
            @Valid @RequestBody AdminStaffStatusChangeRequest request,
            @AuthenticationPrincipal AdminUserPrincipal principal,
            HttpServletRequest servletRequest
    ) {
        return ApiRestResponse.success(staffManagementService.changeStatus(
                request.toCommand(userId, principal.getUserId(), servletRequest.getRemoteAddr())
        ));
    }

    @Operation(summary = "重置员工登录密码")
    @PutMapping("/{userId}/password")
    public ApiRestResponse<Object> resetPassword(
            @PathVariable Long userId,
            @Valid @RequestBody AdminStaffPasswordResetRequest request,
            @AuthenticationPrincipal AdminUserPrincipal principal,
            HttpServletRequest servletRequest
    ) {
        staffManagementService.resetPassword(
                request.toCommand(userId, principal.getUserId(), servletRequest.getRemoteAddr())
        );
        return ApiRestResponse.success();
    }

    @Operation(summary = "解除员工登录临时锁定")
    @PutMapping("/{userId}/unlock")
    public ApiRestResponse<AdminStaffManagementView> unlock(
            @PathVariable Long userId,
            @AuthenticationPrincipal AdminUserPrincipal principal,
            HttpServletRequest servletRequest
    ) {
        return ApiRestResponse.success(staffManagementService.unlock(new AdminStaffUnlockCommand(
                userId,
                principal.getUserId(),
                servletRequest.getRemoteAddr()
        )));
    }
}
