package com.core.coreboot.platform.customer.model;

public record ConsumePinResetTokenResult(
        String resetToken,
        long expiresInSeconds
) {
}
