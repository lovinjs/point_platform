package com.core.coreboot.platform.consumption.controller;

import com.core.coreboot.common.ApiRestResponse;
import com.core.coreboot.platform.auth.security.AdminUserPrincipal;
import com.core.coreboot.platform.consumption.model.AdminConsumptionOrderStatusView;
import com.core.coreboot.platform.consumption.model.AdminConsumptionReversalRequest;
import com.core.coreboot.platform.consumption.model.AdminPrepareConsumptionRequest;
import com.core.coreboot.platform.consumption.model.ConsumptionReversalResult;
import com.core.coreboot.platform.consumption.model.PrepareConsumptionResult;
import com.core.coreboot.platform.consumption.service.AdminConsumptionOrderQueryService;
import com.core.coreboot.platform.consumption.service.ConsumptionReversalService;
import com.core.coreboot.platform.consumption.service.PrepareConsumptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "积分平台 - 后台消费")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/consumption-orders")
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'STORE_MANAGER', 'CLERK')")
public class AdminConsumptionController {
    private final PrepareConsumptionService prepareConsumptionService;
    private final AdminConsumptionOrderQueryService orderQueryService;
    private final ConsumptionReversalService consumptionReversalService;

    @Operation(summary = "创建待客户 PIN 确认的消费订单")
    @PostMapping("/prepare")
    public ApiRestResponse<PrepareConsumptionResult> prepare(
            @Valid @RequestBody AdminPrepareConsumptionRequest request,
            @AuthenticationPrincipal AdminUserPrincipal principal,
            @Parameter(description = "请求唯一键，重试时必须保持不变", required = true)
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            HttpServletRequest servletRequest
    ) {
        return ApiRestResponse.success(prepareConsumptionService.prepare(
                request.toCommand(principal.getUserId(), idempotencyKey, servletRequest.getRemoteAddr())
        ));
    }

    @Operation(summary = "查询当前账号门店范围内的消费订单状态")
    @GetMapping("/{orderNo}")
    public ApiRestResponse<AdminConsumptionOrderStatusView> status(
            @PathVariable String orderNo,
            @AuthenticationPrincipal AdminUserPrincipal principal
    ) {
        return ApiRestResponse.success(orderQueryService.getStatus(principal.getUserId(), orderNo));
    }

    @Operation(summary = "超级管理员冲正已完成的消费订单")
    @PostMapping("/{orderNo}/reversal")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiRestResponse<ConsumptionReversalResult> reverse(
            @PathVariable String orderNo,
            @Valid @RequestBody AdminConsumptionReversalRequest request,
            @AuthenticationPrincipal AdminUserPrincipal principal,
            @Parameter(description = "请求唯一键，重试时必须保持不变", required = true)
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            HttpServletRequest servletRequest
    ) {
        return ApiRestResponse.success(consumptionReversalService.reverse(request.toCommand(
                orderNo,
                principal.getUserId(),
                idempotencyKey,
                servletRequest.getRemoteAddr()
        )));
    }
}
