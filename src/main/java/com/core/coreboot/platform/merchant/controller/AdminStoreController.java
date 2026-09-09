package com.core.coreboot.platform.merchant.controller;

import com.core.coreboot.common.ApiRestResponse;
import com.core.coreboot.platform.auth.security.AdminUserPrincipal;
import com.core.coreboot.platform.common.enums.StoreStatus;
import com.core.coreboot.platform.common.model.PageResult;
import com.core.coreboot.platform.merchant.model.AdminMerchantOptionView;
import com.core.coreboot.platform.merchant.model.AdminStoreCreateRequest;
import com.core.coreboot.platform.merchant.model.AdminStoreManagementView;
import com.core.coreboot.platform.merchant.model.AdminStoreStatusChangeRequest;
import com.core.coreboot.platform.merchant.model.AdminStoreUpdateRequest;
import com.core.coreboot.platform.merchant.model.AdminStoreView;
import com.core.coreboot.platform.merchant.service.AdminStoreManagementService;
import com.core.coreboot.platform.merchant.service.AdminStoreQueryService;
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

@Tag(name = "积分平台 - 后台门店")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/stores")
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'STORE_MANAGER', 'CLERK')")
public class AdminStoreController {
    private final AdminStoreQueryService storeQueryService;
    private final AdminStoreManagementService storeManagementService;

    @Operation(summary = "查询当前账号可操作的有效门店")
    @GetMapping
    public ApiRestResponse<List<AdminStoreView>> list(
            @AuthenticationPrincipal AdminUserPrincipal principal
    ) {
        return ApiRestResponse.success(storeQueryService.listAccessibleActiveStores(principal.getUserId()));
    }

    @Operation(summary = "分页查询全部门店资料")
    @GetMapping("/page")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiRestResponse<PageResult<AdminStoreManagementView>> page(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) Long merchantId,
            @RequestParam(required = false) StoreStatus status,
            @RequestParam(required = false) String keyword,
            @AuthenticationPrincipal AdminUserPrincipal principal
    ) {
        return ApiRestResponse.success(storeManagementService.list(
                principal.getUserId(),
                pageNum,
                pageSize,
                merchantId,
                status,
                keyword
        ));
    }

    @Operation(summary = "查询可用于新建门店的有效合作商户")
    @GetMapping("/merchant-options")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiRestResponse<List<AdminMerchantOptionView>> merchantOptions(
            @AuthenticationPrincipal AdminUserPrincipal principal
    ) {
        return ApiRestResponse.success(
                storeManagementService.listActiveMerchantOptions(principal.getUserId())
        );
    }

    @Operation(summary = "新建合作门店")
    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiRestResponse<AdminStoreManagementView> create(
            @Valid @RequestBody AdminStoreCreateRequest request,
            @AuthenticationPrincipal AdminUserPrincipal principal,
            HttpServletRequest servletRequest
    ) {
        return ApiRestResponse.success(storeManagementService.create(
                request.toCommand(principal.getUserId(), servletRequest.getRemoteAddr())
        ));
    }

    @Operation(summary = "修改门店基本资料")
    @PutMapping("/{storeId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiRestResponse<AdminStoreManagementView> update(
            @PathVariable Long storeId,
            @Valid @RequestBody AdminStoreUpdateRequest request,
            @AuthenticationPrincipal AdminUserPrincipal principal,
            HttpServletRequest servletRequest
    ) {
        return ApiRestResponse.success(storeManagementService.update(
                request.toCommand(storeId, principal.getUserId(), servletRequest.getRemoteAddr())
        ));
    }

    @Operation(summary = "启用或停用门店")
    @PutMapping("/{storeId}/status")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiRestResponse<AdminStoreManagementView> changeStatus(
            @PathVariable Long storeId,
            @Valid @RequestBody AdminStoreStatusChangeRequest request,
            @AuthenticationPrincipal AdminUserPrincipal principal,
            HttpServletRequest servletRequest
    ) {
        return ApiRestResponse.success(storeManagementService.changeStatus(
                request.toCommand(storeId, principal.getUserId(), servletRequest.getRemoteAddr())
        ));
    }
}
