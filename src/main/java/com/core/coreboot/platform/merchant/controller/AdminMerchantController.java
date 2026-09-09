package com.core.coreboot.platform.merchant.controller;

import com.core.coreboot.common.ApiRestResponse;
import com.core.coreboot.platform.auth.security.AdminUserPrincipal;
import com.core.coreboot.platform.common.enums.MerchantStatus;
import com.core.coreboot.platform.common.model.PageResult;
import com.core.coreboot.platform.merchant.model.AdminMerchantCreateRequest;
import com.core.coreboot.platform.merchant.model.AdminMerchantManagementView;
import com.core.coreboot.platform.merchant.model.AdminMerchantStatusChangeRequest;
import com.core.coreboot.platform.merchant.model.AdminMerchantUpdateRequest;
import com.core.coreboot.platform.merchant.service.AdminMerchantManagementService;
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

@Tag(name = "积分平台 - 后台合作商户")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/merchants")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AdminMerchantController {
    private final AdminMerchantManagementService merchantManagementService;

    @Operation(summary = "分页查询合作商户资料")
    @GetMapping
    public ApiRestResponse<PageResult<AdminMerchantManagementView>> page(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) MerchantStatus status,
            @RequestParam(required = false) String keyword,
            @AuthenticationPrincipal AdminUserPrincipal principal
    ) {
        return ApiRestResponse.success(merchantManagementService.list(
                principal.getUserId(), pageNum, pageSize, status, keyword
        ));
    }

    @Operation(summary = "新建合作商户")
    @PostMapping
    public ApiRestResponse<AdminMerchantManagementView> create(
            @Valid @RequestBody AdminMerchantCreateRequest request,
            @AuthenticationPrincipal AdminUserPrincipal principal,
            HttpServletRequest servletRequest
    ) {
        return ApiRestResponse.success(merchantManagementService.create(
                request.toCommand(principal.getUserId(), servletRequest.getRemoteAddr())
        ));
    }

    @Operation(summary = "修改合作商户资料")
    @PutMapping("/{merchantId}")
    public ApiRestResponse<AdminMerchantManagementView> update(
            @PathVariable Long merchantId,
            @Valid @RequestBody AdminMerchantUpdateRequest request,
            @AuthenticationPrincipal AdminUserPrincipal principal,
            HttpServletRequest servletRequest
    ) {
        return ApiRestResponse.success(merchantManagementService.update(
                request.toCommand(merchantId, principal.getUserId(), servletRequest.getRemoteAddr())
        ));
    }

    @Operation(summary = "启用或停用合作商户")
    @PutMapping("/{merchantId}/status")
    public ApiRestResponse<AdminMerchantManagementView> changeStatus(
            @PathVariable Long merchantId,
            @Valid @RequestBody AdminMerchantStatusChangeRequest request,
            @AuthenticationPrincipal AdminUserPrincipal principal,
            HttpServletRequest servletRequest
    ) {
        return ApiRestResponse.success(merchantManagementService.changeStatus(
                request.toCommand(merchantId, principal.getUserId(), servletRequest.getRemoteAddr())
        ));
    }
}
