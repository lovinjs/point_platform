package com.core.coreboot.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import jakarta.validation.constraints.NotNull;

@Getter
@Setter
@Schema(description = "批量更新商品状态参数")
public class BatchUpdateStatusReq {
    @Schema(description = "批量上下架商品ids数组", example = "[1,2,3]")
    @NotNull
    private Integer[] ids;

    @Schema(description = "商品状态，1-上架，0-下架", example = "1")
    @NotNull
    private Integer status;
}
