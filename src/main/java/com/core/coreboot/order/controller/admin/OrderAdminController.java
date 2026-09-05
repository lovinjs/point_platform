package com.core.coreboot.order.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.core.coreboot.common.ApiRestResponse;
import com.core.coreboot.common.dto.BasePageReq;
import com.core.coreboot.order.vo.OrderVO;
import com.core.coreboot.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@Tag(name = "订单模块 - 管理员", description = "订单模块")
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/order")
public class OrderAdminController {

    private final OrderService orderService;

    @Operation(summary = "管理员订单列表", description = "管理员订单列表")
    @GetMapping("/list")
    public ApiRestResponse<Object> adminList(@Valid BasePageReq basePageReq) {
        IPage<OrderVO> page = orderService.listForAdmin(basePageReq);
        return ApiRestResponse.success(page);
    }

    @Operation(summary = "管理员订单发货", description = "管理员订单发货")
    @PostMapping("/shipped")
    public ApiRestResponse<Object> shipped(@Parameter(description = "订单号") @RequestParam String orderNo) {
        orderService.shipped(orderNo);
        return ApiRestResponse.success();
    }
}
