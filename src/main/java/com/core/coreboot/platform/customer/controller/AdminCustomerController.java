package com.core.coreboot.platform.customer.controller;

import com.core.coreboot.common.ApiRestResponse;
import com.core.coreboot.platform.auth.security.AdminUserPrincipal;
import com.core.coreboot.platform.common.enums.CustomerStatus;
import com.core.coreboot.platform.common.model.PageResult;
import com.core.coreboot.platform.consumption.model.CustomerConsumptionOrderView;
import com.core.coreboot.platform.customer.model.AdminCustomerManagementView;
import com.core.coreboot.platform.customer.model.AdminCustomerStatusChangeRequest;
import com.core.coreboot.platform.customer.model.AdminCustomerView;
import com.core.coreboot.platform.customer.service.AdminCustomerManagementService;
import com.core.coreboot.platform.customer.service.AdminCustomerQueryService;
import com.core.coreboot.platform.point.model.CustomerPointLedgerView;
import com.core.coreboot.platform.recharge.model.CustomerRechargeOrderView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
    private final AdminCustomerManagementService customerManagementService;

    @Operation(summary = "按手机号精确查询客户及积分余额")
    @GetMapping
    public ApiRestResponse<AdminCustomerView> findByPhone(
            @RequestParam(required = false) String phone
    ) {
        return ApiRestResponse.success(customerQueryService.findByPhone(phone));
    }

    @Operation(summary = "分页查询平台客户")
    @GetMapping("/page")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiRestResponse<PageResult<AdminCustomerManagementView>> page(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) CustomerStatus status,
            @RequestParam(required = false) String keyword,
            @AuthenticationPrincipal AdminUserPrincipal principal
    ) {
        return ApiRestResponse.success(customerManagementService.list(
                principal.getUserId(),
                pageNum,
                pageSize,
                status,
                keyword
        ));
    }

    @Operation(summary = "查询客户详情")
    @GetMapping("/{customerId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiRestResponse<AdminCustomerManagementView> detail(
            @PathVariable Long customerId,
            @AuthenticationPrincipal AdminUserPrincipal principal
    ) {
        return ApiRestResponse.success(
                customerManagementService.get(principal.getUserId(), customerId)
        );
    }

    @Operation(summary = "分页查询客户积分流水")
    @GetMapping("/{customerId}/point-ledgers")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiRestResponse<PageResult<CustomerPointLedgerView>> pointLedgers(
            @PathVariable Long customerId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @AuthenticationPrincipal AdminUserPrincipal principal
    ) {
        return ApiRestResponse.success(customerManagementService.getPointLedger(
                principal.getUserId(), customerId, pageNum, pageSize
        ));
    }

    @Operation(summary = "分页查询客户充值订单")
    @GetMapping("/{customerId}/recharge-orders")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiRestResponse<PageResult<CustomerRechargeOrderView>> rechargeOrders(
            @PathVariable Long customerId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @AuthenticationPrincipal AdminUserPrincipal principal
    ) {
        return ApiRestResponse.success(customerManagementService.getRechargeOrders(
                principal.getUserId(), customerId, pageNum, pageSize
        ));
    }

    @Operation(summary = "分页查询客户消费订单")
    @GetMapping("/{customerId}/consumption-orders")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiRestResponse<PageResult<CustomerConsumptionOrderView>> consumptionOrders(
            @PathVariable Long customerId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @AuthenticationPrincipal AdminUserPrincipal principal
    ) {
        return ApiRestResponse.success(customerManagementService.getConsumptionOrders(
                principal.getUserId(), customerId, pageNum, pageSize
        ));
    }

    @Operation(summary = "冻结或恢复客户账户")
    @PutMapping("/{customerId}/status")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiRestResponse<AdminCustomerManagementView> changeStatus(
            @PathVariable Long customerId,
            @Valid @RequestBody AdminCustomerStatusChangeRequest request,
            @AuthenticationPrincipal AdminUserPrincipal principal,
            HttpServletRequest servletRequest
    ) {
        return ApiRestResponse.success(customerManagementService.changeStatus(
                request.toCommand(
                        customerId,
                        principal.getUserId(),
                        servletRequest.getRemoteAddr()
                )
        ));
    }
}
