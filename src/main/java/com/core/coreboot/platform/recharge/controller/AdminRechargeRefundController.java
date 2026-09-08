package com.core.coreboot.platform.recharge.controller;

import com.core.coreboot.common.ApiRestResponse;
import com.core.coreboot.platform.auth.security.AdminUserPrincipal;
import com.core.coreboot.platform.recharge.model.AdminRechargeRefundRequest;
import com.core.coreboot.platform.recharge.model.RechargeRefundResult;
import com.core.coreboot.platform.recharge.service.RechargeRefundService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "积分平台 - 后台充值退款")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/recharge-orders")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AdminRechargeRefundController {
    private final RechargeRefundService rechargeRefundService;

    @Operation(summary = "登记实际退款并整笔退回未消费充值")
    @PostMapping("/{orderNo}/refund")
    public ApiRestResponse<RechargeRefundResult> refund(
            @PathVariable String orderNo,
            @Valid @RequestBody AdminRechargeRefundRequest request,
            @AuthenticationPrincipal AdminUserPrincipal principal,
            @Parameter(description = "请求唯一键，重试时必须保持不变", required = true)
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            HttpServletRequest servletRequest
    ) {
        return ApiRestResponse.success(rechargeRefundService.refund(request.toCommand(
                orderNo,
                principal.getUserId(),
                idempotencyKey,
                servletRequest.getRemoteAddr()
        )));
    }
}
