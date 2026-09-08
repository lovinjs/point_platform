package com.core.coreboot.platform.consumption.controller;

import com.core.coreboot.common.ApiRestResponse;
import com.core.coreboot.platform.consumption.model.ConfirmConsumptionRequest;
import com.core.coreboot.platform.consumption.model.ConsumptionConfirmationResult;
import com.core.coreboot.platform.consumption.model.CustomerPendingConsumptionView;
import com.core.coreboot.platform.consumption.service.CustomerConsumptionService;
import com.core.coreboot.platform.customer.auth.security.CustomerPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "积分平台 - 客户消费确认")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/customer/consumption-orders")
public class CustomerConsumptionController {
    private final CustomerConsumptionService customerConsumptionService;

    @Operation(summary = "查询当前客户的待确认消费订单")
    @GetMapping("/pending")
    public ApiRestResponse<CustomerPendingConsumptionView> pending(
            @AuthenticationPrincipal CustomerPrincipal principal
    ) {
        return ApiRestResponse.success(customerConsumptionService.getPending(principal.getCustomerId()));
    }

    @Operation(summary = "当前客户输入消费密码确认消费")
    @PostMapping("/{orderNo}/confirm")
    public ApiRestResponse<ConsumptionConfirmationResult> confirm(
            @PathVariable String orderNo,
            @Valid @RequestBody ConfirmConsumptionRequest request,
            @AuthenticationPrincipal CustomerPrincipal principal,
            HttpServletRequest servletRequest
    ) {
        return ApiRestResponse.success(customerConsumptionService.confirm(
                principal.getCustomerId(),
                orderNo,
                request.consumePin(),
                servletRequest.getRemoteAddr()
        ));
    }
}
