package com.core.coreboot.platform.customer.auth.controller;

import com.core.coreboot.common.ApiRestResponse;
import com.core.coreboot.platform.customer.auth.model.CustomerAccountView;
import com.core.coreboot.platform.customer.auth.security.CustomerPrincipal;
import com.core.coreboot.platform.customer.auth.service.CustomerProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "积分平台 - 客户账户")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/customer")
public class CustomerProfileController {
    private final CustomerProfileService customerProfileService;

    @Operation(summary = "查询当前客户账户与积分余额")
    @GetMapping("/me")
    public ApiRestResponse<CustomerAccountView> me(
            @AuthenticationPrincipal CustomerPrincipal principal
    ) {
        return ApiRestResponse.success(customerProfileService.getProfile(principal.getCustomerId()));
    }
}
