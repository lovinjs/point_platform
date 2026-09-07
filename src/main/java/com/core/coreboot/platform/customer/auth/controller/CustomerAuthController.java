package com.core.coreboot.platform.customer.auth.controller;

import com.core.coreboot.common.ApiRestResponse;
import com.core.coreboot.platform.customer.auth.model.CustomerSessionResponse;
import com.core.coreboot.platform.customer.auth.model.ExchangeCustomerLoginTicketRequest;
import com.core.coreboot.platform.customer.auth.service.CustomerSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "积分平台 - 客户登录")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/h5/auth")
public class CustomerAuthController {
    private final CustomerSessionService customerSessionService;

    @Operation(summary = "使用一次性登录票据换取客户访问令牌")
    @PostMapping("/session/exchange")
    public ApiRestResponse<CustomerSessionResponse> exchangeLoginTicket(
            @Valid @RequestBody ExchangeCustomerLoginTicketRequest request
    ) {
        return ApiRestResponse.success(customerSessionService.exchangeLoginTicket(request.ticket()));
    }
}
