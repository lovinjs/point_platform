package com.core.coreboot.platform.order.controller;

import com.core.coreboot.common.ApiRestResponse;
import com.core.coreboot.platform.auth.security.AdminUserPrincipal;
import com.core.coreboot.platform.common.enums.ConsumptionOrderStatus;
import com.core.coreboot.platform.common.enums.RechargeOrderStatus;
import com.core.coreboot.platform.common.model.PageResult;
import com.core.coreboot.platform.order.model.AdminConsumptionOrderView;
import com.core.coreboot.platform.order.model.AdminOrderStoreOptionView;
import com.core.coreboot.platform.order.model.AdminRechargeOrderView;
import com.core.coreboot.platform.order.service.AdminOrderQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "积分平台 - 后台订单中心")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/order-center")
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'STORE_MANAGER', 'CLERK')")
public class AdminOrderQueryController {
    private final AdminOrderQueryService orderQueryService;

    @Operation(summary = "查询当前账号可查看的历史订单门店")
    @GetMapping("/store-options")
    public ApiRestResponse<List<AdminOrderStoreOptionView>> storeOptions(
            @AuthenticationPrincipal AdminUserPrincipal principal
    ) {
        return ApiRestResponse.success(orderQueryService.listStoreOptions(principal.getUserId()));
    }

    @Operation(summary = "分页查询当前账号门店范围内的充值订单")
    @GetMapping("/recharge-orders")
    public ApiRestResponse<PageResult<AdminRechargeOrderView>> rechargeOrders(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) Long storeId,
            @RequestParam(required = false) RechargeOrderStatus status,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) String customerPhone,
            @AuthenticationPrincipal AdminUserPrincipal principal
    ) {
        return ApiRestResponse.success(orderQueryService.listRechargeOrders(
                principal.getUserId(), pageNum, pageSize, storeId, status, orderNo, customerPhone
        ));
    }

    @Operation(summary = "分页查询当前账号门店范围内的消费订单")
    @GetMapping("/consumption-orders")
    public ApiRestResponse<PageResult<AdminConsumptionOrderView>> consumptionOrders(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) Long storeId,
            @RequestParam(required = false) ConsumptionOrderStatus status,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) String customerPhone,
            @AuthenticationPrincipal AdminUserPrincipal principal
    ) {
        return ApiRestResponse.success(orderQueryService.listConsumptionOrders(
                principal.getUserId(), pageNum, pageSize, storeId, status, orderNo, customerPhone
        ));
    }
}
