package com.core.coreboot.common.dto;

import lombok.Data;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 基础分页请求对象
 */
@Data
@Schema(description = "基础分页请求对象")
public class BasePageReq {
    private static final int MAX_PAGE_SIZE = 100;

    @Schema(description = "分页页数", example = "1")
    @NotNull
    @Min(1)
    private Integer pageNum = 1;

    @Schema(description = "分页大小", example = "10")
    @NotNull
    @Min(1)
    @Max(MAX_PAGE_SIZE)
    private Integer pageSize = 10;

    public <T> Page<T> getPaginationPage() {
        if (pageNum == null || pageNum < 1) {
            pageNum = 1;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = 10;
        }
        if (pageSize > MAX_PAGE_SIZE) {
            pageSize = MAX_PAGE_SIZE;
        }
        return new Page<>(pageNum, pageSize);
    }
}
