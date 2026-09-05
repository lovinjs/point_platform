package com.core.coreboot.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Getter
@Setter
@Schema(description = "更新商品参数")
public class UpdateProductReq {

    @Schema(description = "商品id", example = "1")
    @NotNull
    private Integer id;

    @Schema(description = "商品名称", example = "苹果")
    @Size(min=1, max=12)
    @NotNull
    private String name;

    @Schema(description = "商品图片", example = "")
    @NotNull
    private String image;

    @Schema(description = "商品详情", example = "好吃的红苹果")
    private String detail;

    @Schema(description = "商品分类id", example = "1")
    @NotNull
    private Integer categoryId;

    @Schema(description = "商品价格", example = "1")
    @NotNull
    @Min(1)
    private Integer price;

    @Schema(description = "商品库存", example = "1")
    @NotNull
    @Min(1)
    @Max(10000)
    private Integer stock;

    @Schema(description = "虚拟销量", example = "1")
    @NotNull
    @Min(1)
    @Max(10000)
    private Integer virtualSale;

    @Schema(description = "商品状态", example = "1")
    @NotNull
    private Integer status;
}