package com.core.coreboot.product.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.core.coreboot.common.ApiRestResponse;
import com.core.coreboot.product.dto.BatchUpdateStatusReq;
import com.core.coreboot.product.dto.ProductListReq;
import com.core.coreboot.product.entity.Product;
import com.core.coreboot.product.dto.AddProductReq;
import com.core.coreboot.product.dto.UpdateProductReq;
import com.core.coreboot.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@Tag(name = "商品模块 - 管理员", description = "商品模块")
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/product")
public class ProductAdminController {

    private final ProductService productService;

    @Operation(summary = "添加商品", description = "添加商品")
    @PostMapping("/add")
    public ApiRestResponse<Object> addProduct(@Valid @RequestBody AddProductReq addProductReq) {
        productService.add(addProductReq);
        return ApiRestResponse.success();
    }

    @Operation(summary = "更新商品", description = "更新商品")
    @PostMapping("/update")
    public ApiRestResponse<Object> updateProduct(@Valid @RequestBody UpdateProductReq updateProductReq) {
        productService.update(updateProductReq);
        return ApiRestResponse.success();
    }

    @Operation(summary = "删除商品", description = "删除商品")
    @PostMapping("/delete")
    public ApiRestResponse<Object> deleteProduct(@Parameter(description = "商品id") @Valid @RequestParam Integer id) {
        productService.delete(id);
        return ApiRestResponse.success();
    }

    @Operation(summary = "管理员查询商品列表", description = "管理员查询商品列表")
    @PostMapping("/list")
    @ResponseBody
    public ApiRestResponse<Object> getProductListForAdmin(@ParameterObject @Valid ProductListReq productListReq) {
        IPage<Product> page = productService.getListForAdmin(productListReq);
        return ApiRestResponse.success(page);
    }

    @Operation(summary = "批量上下架商品", description = "批量上下架商品")
    @PostMapping("/batchUpdateStatus")
    public ApiRestResponse<Object> batchUpdateProductStatus(@ParameterObject @Valid @RequestBody BatchUpdateStatusReq batchUpdateStatusReq) {
        productService.batchUpdateStatus(batchUpdateStatusReq.getIds(), batchUpdateStatusReq.getStatus());
        return ApiRestResponse.success();
    }
}
