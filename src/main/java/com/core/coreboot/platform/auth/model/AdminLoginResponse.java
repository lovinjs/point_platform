package com.core.coreboot.platform.auth.model;

import java.time.Instant;

public record AdminLoginResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds,
        Instant expiresAt,
        AdminAccountView user
) {
}
