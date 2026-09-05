package com.core.coreboot.category.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Setter
@Getter
@Schema(description = "添加分类参数")
public class AddCategoryReq {

    @Schema(description = "分类名称", example = "水果")
    @Size(min=2, max=6)
    @NotNull
    private String name;

    @Schema(description = "分类等级", example = "1")
    @Max(3)
    @NotNull
    private Integer rank;

    @Schema(description = "分类排序", example = "1")
    @NotNull
    private Integer order;

    @Schema(description = "分类父id", example = "1")
    @NotNull
    private Integer parentId;

}
