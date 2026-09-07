package com.core.coreboot.platform.customer.controller;

import com.core.coreboot.common.ApiRestResponse;
import com.core.coreboot.platform.customer.model.AdminCustomerView;
import com.core.coreboot.platform.customer.service.AdminCustomerQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "积分平台 - 后台客户")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/customers")
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'STORE_MANAGER', 'CLERK')")
public class AdminCustomerController {
    private final AdminCustomerQueryService customerQueryService;

    @Operation(summary = "按手机号精确查询客户及积分余额")
    @GetMapping
    public ApiRestResponse<AdminCustomerView> findByPhone(
            @RequestParam(required = false) String phone
    ) {
        return ApiRestResponse.success(customerQueryService.findByPhone(phone));
    }
}
