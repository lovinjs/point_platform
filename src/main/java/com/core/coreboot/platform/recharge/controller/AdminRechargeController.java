package com.core.coreboot.platform.recharge.controller;

import com.core.coreboot.common.ApiRestResponse;
import com.core.coreboot.platform.auth.security.AdminUserPrincipal;
import com.core.coreboot.platform.recharge.model.AdminOfflineRechargeRequest;
import com.core.coreboot.platform.recharge.model.OfflineRechargeResult;
import com.core.coreboot.platform.recharge.service.OfflineRechargeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "积分平台 - 后台充值")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/recharge-orders")
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'STORE_MANAGER', 'CLERK')")
public class AdminRechargeController {
    private final OfflineRechargeService offlineRechargeService;

    @Operation(summary = "确认平台收款并完成线下充值")
    @PostMapping("/offline")
    public ApiRestResponse<OfflineRechargeResult> offlineRecharge(
            @Valid @RequestBody AdminOfflineRechargeRequest request,
            @AuthenticationPrincipal AdminUserPrincipal principal,
            @Parameter(description = "请求唯一键，重试时必须保持不变", required = true)
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            HttpServletRequest servletRequest
    ) {
        return ApiRestResponse.success(offlineRechargeService.recharge(
                request.toCommand(principal.getUserId(), idempotencyKey, servletRequest.getRemoteAddr())
        ));
    }
}
