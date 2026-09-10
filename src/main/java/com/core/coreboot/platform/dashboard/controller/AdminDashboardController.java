package com.core.coreboot.platform.dashboard.controller;

import com.core.coreboot.common.ApiRestResponse;
import com.core.coreboot.platform.auth.security.AdminUserPrincipal;
import com.core.coreboot.platform.dashboard.model.AdminDashboardOverviewView;
import com.core.coreboot.platform.dashboard.service.AdminDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "积分平台 - 经营数据总览")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/dashboard")
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'STORE_MANAGER')")
public class AdminDashboardController {
    private final AdminDashboardService dashboardService;

    @Operation(summary = "查询当前账号权限范围内的经营数据总览")
    @GetMapping("/overview")
    public ApiRestResponse<AdminDashboardOverviewView> overview(
            @RequestParam(required = false) Long storeId,
            @RequestParam(defaultValue = "7") int trendDays,
            @AuthenticationPrincipal AdminUserPrincipal principal
    ) {
        return ApiRestResponse.success(dashboardService.overview(
                principal.getUserId(), storeId, trendDays
        ));
    }
}
