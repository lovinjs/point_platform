package com.core.coreboot.platform.setting.controller;

import com.core.coreboot.common.ApiRestResponse;
import com.core.coreboot.platform.auth.security.AdminUserPrincipal;
import com.core.coreboot.platform.setting.model.PlatformBusinessSettingUpdateRequest;
import com.core.coreboot.platform.setting.model.PlatformBusinessSettingView;
import com.core.coreboot.platform.setting.service.PlatformBusinessSettingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "积分平台 - 平台业务配置")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/platform-settings/business")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AdminPlatformBusinessSettingController {
    private final PlatformBusinessSettingService settingService;

    @Operation(summary = "查询当前平台业务配置")
    @GetMapping
    public ApiRestResponse<PlatformBusinessSettingView> current(
            @AuthenticationPrincipal AdminUserPrincipal principal
    ) {
        return ApiRestResponse.success(settingService.getCurrent(principal.getUserId()));
    }

    @Operation(summary = "修改平台手续费率和消费确认有效期")
    @PutMapping
    public ApiRestResponse<PlatformBusinessSettingView> update(
            @Valid @RequestBody PlatformBusinessSettingUpdateRequest request,
            @AuthenticationPrincipal AdminUserPrincipal principal,
            HttpServletRequest servletRequest
    ) {
        return ApiRestResponse.success(settingService.update(
                request.toCommand(principal.getUserId(), servletRequest.getRemoteAddr())
        ));
    }
}
