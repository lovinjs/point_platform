package com.core.coreboot.platform.customer.phone.controller;

import com.core.coreboot.common.ApiRestResponse;
import com.core.coreboot.platform.customer.auth.model.CustomerAccountView;
import com.core.coreboot.platform.customer.auth.security.CustomerPrincipal;
import com.core.coreboot.platform.customer.phone.model.PhoneBindRequest;
import com.core.coreboot.platform.customer.phone.model.PhoneVerificationCodeRequest;
import com.core.coreboot.platform.customer.phone.model.PhoneVerificationDispatchResult;
import com.core.coreboot.platform.customer.phone.service.CustomerPhoneService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "积分平台 - 客户手机号")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/customer/phone")
public class CustomerPhoneController {
    private final CustomerPhoneService customerPhoneService;

    @Operation(summary = "向待绑定手机号发送验证码")
    @PostMapping("/verification-codes")
    public ApiRestResponse<PhoneVerificationDispatchResult> requestVerificationCode(
            @Valid @RequestBody PhoneVerificationCodeRequest request,
            @AuthenticationPrincipal CustomerPrincipal principal
    ) {
        return ApiRestResponse.success(customerPhoneService.requestVerificationCode(
                principal.getCustomerId(),
                request.phone()
        ));
    }

    @Operation(summary = "使用短信验证码绑定当前客户手机号")
    @PutMapping
    public ApiRestResponse<CustomerAccountView> bindPhone(
            @Valid @RequestBody PhoneBindRequest request,
            @AuthenticationPrincipal CustomerPrincipal principal,
            HttpServletRequest servletRequest
    ) {
        return ApiRestResponse.success(customerPhoneService.bindPhone(
                principal.getCustomerId(),
                request.phone(),
                request.verificationCode(),
                servletRequest.getRemoteAddr()
        ));
    }
}
