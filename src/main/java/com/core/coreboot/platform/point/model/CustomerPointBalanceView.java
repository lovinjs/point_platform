package com.core.coreboot.platform.point.model;

import java.time.LocalDateTime;

public record CustomerPointBalanceView(
        long availablePoints,
        LocalDateTime updatedTime
) {
}
