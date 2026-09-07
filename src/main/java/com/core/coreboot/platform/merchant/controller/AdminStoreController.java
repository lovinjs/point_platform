package com.core.coreboot.platform.merchant.controller;

import com.core.coreboot.common.ApiRestResponse;
import com.core.coreboot.platform.auth.security.AdminUserPrincipal;
import com.core.coreboot.platform.merchant.model.AdminStoreView;
import com.core.coreboot.platform.merchant.service.AdminStoreQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "积分平台 - 后台门店")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/stores")
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'STORE_MANAGER', 'CLERK')")
public class AdminStoreController {
    private final AdminStoreQueryService storeQueryService;

    @Operation(summary = "查询当前账号可操作的有效门店")
    @GetMapping
    public ApiRestResponse<List<AdminStoreView>> list(
            @AuthenticationPrincipal AdminUserPrincipal principal
    ) {
        return ApiRestResponse.success(storeQueryService.listAccessibleActiveStores(principal.getUserId()));
    }
}
