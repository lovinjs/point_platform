package com.core.coreboot.platform.customer.auth.model;

public record CustomerAccountView(
        Long customerId,
        String nickname,
        String avatarUrl,
        String maskedPhone,
        boolean phoneBound,
        boolean consumePinConfigured,
        long availablePoints
) {
}
