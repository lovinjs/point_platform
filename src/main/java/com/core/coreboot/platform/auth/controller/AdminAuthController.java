package com.core.coreboot.platform.auth.controller;

import com.core.coreboot.common.ApiRestResponse;
import com.core.coreboot.platform.auth.model.AdminAccountView;
import com.core.coreboot.platform.auth.model.AdminChangePasswordRequest;
import com.core.coreboot.platform.auth.model.AdminLoginRequest;
import com.core.coreboot.platform.auth.model.AdminLoginResponse;
import com.core.coreboot.platform.auth.security.AdminUserPrincipal;
import com.core.coreboot.platform.auth.service.AdminAuthenticationService;
import com.core.coreboot.platform.auth.service.AdminPasswordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "积分平台 - 后台认证")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/auth")
public class AdminAuthController {
    private final AdminAuthenticationService authenticationService;
    private final AdminPasswordService passwordService;

    @Operation(summary = "后台账号登录")
    @PostMapping("/login")
    public ApiRestResponse<AdminLoginResponse> login(
            @Valid @RequestBody AdminLoginRequest request,
            HttpServletRequest servletRequest
    ) {
        return ApiRestResponse.success(authenticationService.login(request, servletRequest.getRemoteAddr()));
    }

    @Operation(summary = "获取当前后台账号")
    @GetMapping("/me")
    public ApiRestResponse<AdminAccountView> me(@AuthenticationPrincipal AdminUserPrincipal principal) {
        return ApiRestResponse.success(AdminAccountView.from(principal));
    }

    @Operation(summary = "修改当前后台账号的登录密码")
    @PutMapping("/password")
    public ApiRestResponse<Object> changePassword(
            @Valid @RequestBody AdminChangePasswordRequest request,
            @AuthenticationPrincipal AdminUserPrincipal principal,
            HttpServletRequest servletRequest
    ) {
        passwordService.changeOwnPassword(
                principal.getUserId(),
                principal.getRoles(),
                request.currentPassword(),
                request.newPassword(),
                servletRequest.getRemoteAddr()
        );
        return ApiRestResponse.success();
    }
}
