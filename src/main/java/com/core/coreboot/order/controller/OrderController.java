package com.core.coreboot.order.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.core.coreboot.common.ApiRestResponse;
import com.core.coreboot.common.dto.BasePageReq;
import com.core.coreboot.order.dto.CreateOrderReq;
import com.core.coreboot.order.vo.OrderVO;
import com.core.coreboot.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;

import jakarta.validation.Valid;

@Tag(name = "订单模块 - 用户", description = "订单模块")
@RestController
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "创建订单", description = "创建订单")
    @PostMapping("/order/create")
    public ApiRestResponse<Object> create(@Valid @RequestBody CreateOrderReq createOrderReq) {
        String orderNo = orderService.create(createOrderReq);
        return ApiRestResponse.success(orderNo);
    }

    @Operation(summary = "订单详情", description = "查看订单详情")
    @GetMapping("/order/detail")
    public ApiRestResponse<Object> detail(@Parameter(description = "订单号") @RequestParam String orderNo) {
        OrderVO orderVO = orderService.detail(orderNo);
        return ApiRestResponse.success(orderVO);
    }

    @Operation(summary = "取消订单", description = "用户取消订单")
    @PostMapping("/order/cancel")
    public ApiRestResponse<Object> cancel(@Parameter(description = "订单号") @RequestParam String orderNo) {
        orderService.cancel(orderNo);
        return ApiRestResponse.success();
    }

    @Operation(summary = "支付二维码", description = "订单支付二维码")
    @PostMapping("/order/qrcode")
    public ApiRestResponse<Object> qrcode(@Parameter(description = "订单号") @RequestParam String orderNo) {
        String qrcodeAddress = orderService.qrcode(orderNo);
        return ApiRestResponse.success(qrcodeAddress);
    }

    @Operation(summary = "扫码支付", description = "扫描二维码支付")
    @GetMapping("/pay")
    public ApiRestResponse<Object> pay(@Parameter(description = "订单号") @RequestParam String orderNo) {
        orderService.pay(orderNo);
        return ApiRestResponse.success();
    }

    @Operation(summary = "订单完结", description = "订单完结")
    @PostMapping("/order/done")
    public ApiRestResponse<Object> done(@Parameter(description = "订单号") @RequestParam String orderNo) {
        orderService.done(orderNo);
        return ApiRestResponse.success();
    }

    @Operation(summary = "前台订单列表", description = "前台订单列表")
    @GetMapping("/order/list")
    public ApiRestResponse<Object> listForUser(@Valid BasePageReq basePageReq) {
        IPage<OrderVO> page = orderService.listForUser(basePageReq);
        return ApiRestResponse.success(page);
    }
}
