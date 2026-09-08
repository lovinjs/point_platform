package com.core.coreboot.platform.common.model;

import java.util.List;

public record PageResult<T>(
        long pageNum,
        long pageSize,
        long total,
        long totalPages,
        boolean hasNext,
        List<T> items
) {
    public PageResult {
        items = items == null ? List.of() : List.copyOf(items);
    }
}
