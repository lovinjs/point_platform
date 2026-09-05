package com.core.coreboot.product.dto;

import com.core.coreboot.common.dto.BasePageReq;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import jakarta.validation.constraints.Size;

@Getter
@Setter
@Schema(description = "商品列表参数")
public class ProductListReq extends BasePageReq {

    @Schema(description = "商品名称关键字", example = "苹果")
    @Size(min=1, max=12)
    private String keyword;

    @Schema(description = "商品分类id", example = "0")
    private Integer categoryId;

    @Schema(description = "排序策略，0-默认排序, 1-价格升序, 2-价格降序, 3-销量升序，4-销量降序，5-库存升序，6-库存降序", example = "0")
    private Integer sortCode;
}