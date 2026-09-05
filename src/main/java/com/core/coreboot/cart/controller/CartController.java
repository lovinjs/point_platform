package com.core.coreboot.cart.controller;

import com.core.coreboot.common.ApiRestResponse;
import com.core.coreboot.filter.UserRoleFilter;
import com.core.coreboot.cart.vo.CartVO;
import com.core.coreboot.cart.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Tag(name = "购物车模块", description = "购物车模块")
@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @Operation(summary = "购物车列表", description = "查询购物车列表")
    @GetMapping("/list")
    public ApiRestResponse<Object> list() {
        // 内部获取用户id，不允许前端传递
        List<CartVO> cartVOList = cartService.list(UserRoleFilter.getCurrentUser().getId());
        return ApiRestResponse.success(cartVOList);
    }

    @Operation(summary = "添加购物车", description = "添加商品进购物车")
    @PostMapping("/add")
    public ApiRestResponse<Object> add(
        @Parameter(description = "商品id") @RequestParam Integer productId,
        @Parameter(description = "商品数量") @RequestParam Integer count
    ) {
        List<CartVO> cartVOList = cartService.add(UserRoleFilter.getCurrentUser().getId(), productId, count);
        return ApiRestResponse.success(cartVOList);
    }

    @Operation(summary = "更新购物车", description = "更新购物车中的商品")
    @PostMapping("/update")
    public ApiRestResponse<Object> update(
        @Parameter(description = "商品id") @RequestParam Integer productId,
        @Parameter(description = "商品数量") @RequestParam Integer count
    ) {
        List<CartVO> cartVOList = cartService.update(UserRoleFilter.getCurrentUser().getId(), productId, count);
        return ApiRestResponse.success(cartVOList);
    }

    @Operation(summary = "删除购物车", description = "删除购物车中的商品")
    @PostMapping("/delete")
    public ApiRestResponse<Object> delete(@Parameter(description = "商品id") @RequestParam Integer productId) {
        List<CartVO> cartVOList = cartService.delete(UserRoleFilter.getCurrentUser().getId(), productId);
        return ApiRestResponse.success(cartVOList);
    }
}
