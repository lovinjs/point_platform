package com.core.coreboot.platform.customer.auth.model;

import java.time.Instant;

public record CustomerSessionResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds,
        Instant expiresAt,
        CustomerAccountView user,
        String returnPath
) {
}
