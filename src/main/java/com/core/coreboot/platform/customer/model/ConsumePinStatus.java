package com.core.coreboot.platform.customer.model;

import java.time.LocalDateTime;

public record ConsumePinStatus(
        boolean configured,
        boolean locked,
        LocalDateTime lockedUntil,
        LocalDateTime pinUpdatedTime
) {
}
