package com.core.coreboot.platform.customer.controller;

import com.core.coreboot.common.ApiRestResponse;
import com.core.coreboot.platform.common.model.PageResult;
import com.core.coreboot.platform.consumption.model.CustomerConsumptionOrderView;
import com.core.coreboot.platform.customer.auth.security.CustomerPrincipal;
import com.core.coreboot.platform.customer.service.CustomerTransactionQueryService;
import com.core.coreboot.platform.point.model.CustomerPointBalanceView;
import com.core.coreboot.platform.point.model.CustomerPointLedgerView;
import com.core.coreboot.platform.recharge.model.CustomerRechargeOrderView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "积分平台 - 客户账单与订单")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/customer")
public class CustomerTransactionController {
    private final CustomerTransactionQueryService queryService;

    @Operation(summary = "查询当前客户积分余额")
    @GetMapping("/points/balance")
    public ApiRestResponse<CustomerPointBalanceView> balance(
            @AuthenticationPrincipal CustomerPrincipal principal
    ) {
        return ApiRestResponse.success(queryService.getPointBalance(principal.getCustomerId()));
    }

    @Operation(summary = "分页查询当前客户积分流水")
    @GetMapping("/points/ledger")
    public ApiRestResponse<PageResult<CustomerPointLedgerView>> pointLedger(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @AuthenticationPrincipal CustomerPrincipal principal
    ) {
        return ApiRestResponse.success(queryService.getPointLedger(
                principal.getCustomerId(),
                pageNum,
                pageSize
        ));
    }

    @Operation(summary = "分页查询当前客户充值订单")
    @GetMapping("/recharge-orders")
    public ApiRestResponse<PageResult<CustomerRechargeOrderView>> rechargeOrders(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @AuthenticationPrincipal CustomerPrincipal principal
    ) {
        return ApiRestResponse.success(queryService.getRechargeOrders(
                principal.getCustomerId(),
                pageNum,
                pageSize
        ));
    }

    @Operation(summary = "分页查询当前客户消费订单")
    @GetMapping("/consumption-orders")
    public ApiRestResponse<PageResult<CustomerConsumptionOrderView>> consumptionOrders(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @AuthenticationPrincipal CustomerPrincipal principal
    ) {
        return ApiRestResponse.success(queryService.getConsumptionOrders(
                principal.getCustomerId(),
                pageNum,
                pageSize
        ));
    }
}
