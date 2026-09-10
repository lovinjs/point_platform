package com.core.coreboot.platform.customer.controller;

import com.core.coreboot.common.ApiRestResponse;
import com.core.coreboot.platform.customer.auth.security.CustomerPrincipal;
import com.core.coreboot.platform.customer.model.ChangeConsumePinRequest;
import com.core.coreboot.platform.customer.model.ConsumePinResetTokenResult;
import com.core.coreboot.platform.customer.model.ConsumePinResetVerificationRequest;
import com.core.coreboot.platform.customer.model.ConsumePinStatus;
import com.core.coreboot.platform.customer.model.ResetConsumePinRequest;
import com.core.coreboot.platform.customer.model.SetConsumePinRequest;
import com.core.coreboot.platform.customer.phone.model.PhoneVerificationDispatchResult;
import com.core.coreboot.platform.customer.service.CustomerConsumePinRecoveryService;
import com.core.coreboot.platform.customer.service.CustomerConsumePinService;
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

@Tag(name = "积分平台 - 客户消费密码")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/customer/security/consume-pin")
public class CustomerConsumePinController {
    private final CustomerConsumePinService customerConsumePinService;
    private final CustomerConsumePinRecoveryService recoveryService;

    @Operation(summary = "查询当前客户消费密码状态")
    @GetMapping("/status")
    public ApiRestResponse<ConsumePinStatus> status(
            @AuthenticationPrincipal CustomerPrincipal principal
    ) {
        return ApiRestResponse.success(customerConsumePinService.getStatus(principal.getCustomerId()));
    }

    @Operation(summary = "首次设置当前客户消费密码")
    @PostMapping
    public ApiRestResponse<ConsumePinStatus> setInitialPin(
            @Valid @RequestBody SetConsumePinRequest request,
            @AuthenticationPrincipal CustomerPrincipal principal,
            HttpServletRequest servletRequest
    ) {
        Long customerId = principal.getCustomerId();
        customerConsumePinService.setInitialPin(customerId, request.newPin(), servletRequest.getRemoteAddr());
        return ApiRestResponse.success(customerConsumePinService.getStatus(customerId));
    }

    @Operation(summary = "使用当前密码修改消费密码")
    @PutMapping
    public ApiRestResponse<ConsumePinStatus> changePin(
            @Valid @RequestBody ChangeConsumePinRequest request,
            @AuthenticationPrincipal CustomerPrincipal principal,
            HttpServletRequest servletRequest
    ) {
        Long customerId = principal.getCustomerId();
        customerConsumePinService.changePin(
                customerId,
                request.currentPin(),
                request.newPin(),
                servletRequest.getRemoteAddr()
        );
        return ApiRestResponse.success(customerConsumePinService.getStatus(customerId));
    }

    @Operation(summary = "向当前客户已绑定手机号发送消费密码重置验证码")
    @PostMapping("/reset/verification-codes")
    public ApiRestResponse<PhoneVerificationDispatchResult> requestResetVerificationCode(
            @AuthenticationPrincipal CustomerPrincipal principal
    ) {
        return ApiRestResponse.success(recoveryService.requestVerificationCode(principal.getCustomerId()));
    }

    @Operation(summary = "验证短信验证码并签发一次性消费密码重置凭证")
    @PostMapping("/reset/tokens")
    public ApiRestResponse<ConsumePinResetTokenResult> issueResetToken(
            @Valid @RequestBody ConsumePinResetVerificationRequest request,
            @AuthenticationPrincipal CustomerPrincipal principal
    ) {
        return ApiRestResponse.success(recoveryService.verifyAndIssueResetToken(
                principal.getCustomerId(),
                request.verificationCode()
        ));
    }

    @Operation(summary = "使用一次性凭证重置消费密码并取消待确认消费订单")
    @PutMapping("/reset")
    public ApiRestResponse<ConsumePinStatus> resetPin(
            @Valid @RequestBody ResetConsumePinRequest request,
            @AuthenticationPrincipal CustomerPrincipal principal,
            HttpServletRequest servletRequest
    ) {
        Long customerId = principal.getCustomerId();
        recoveryService.resetPin(customerId, request.resetToken(), request.newPin(), servletRequest.getRemoteAddr());
        return ApiRestResponse.success(customerConsumePinService.getStatus(customerId));
    }
}
