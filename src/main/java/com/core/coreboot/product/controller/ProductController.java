package com.core.coreboot.product.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.core.coreboot.common.ApiRestResponse;
import com.core.coreboot.product.dto.ProductListReq;
import com.core.coreboot.product.service.ProductService;
import com.core.coreboot.product.vo.ProductVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;

import jakarta.validation.Valid;

@Tag(name = "商品模块 - 用户", description = "商品模块")
@RestController
@RequiredArgsConstructor
@RequestMapping("/product")
public class ProductController {

    private final ProductService productService;

    @Operation(summary = "商品详情", description = "商品详情")
    @GetMapping("/detail")
    public ApiRestResponse<Object> productDetail(@Parameter(description = "商品id") @Valid @RequestParam Integer id) {
        ProductVO productVO = productService.getProductDetailForUser(id);
        return ApiRestResponse.success(productVO);
    }

    @Operation(summary = "用户查询商品列表", description = "用户查询商品列表")
    @GetMapping("/list")
    @ResponseBody
    public ApiRestResponse<Object> getProductListForUser(@ParameterObject @Valid ProductListReq productListReq) {
        IPage<ProductVO> page = productService.getListForUser(productListReq);
        return ApiRestResponse.success(page);
    }
}
