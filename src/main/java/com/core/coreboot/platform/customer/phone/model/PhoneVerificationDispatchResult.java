package com.core.coreboot.platform.customer.phone.model;

public record PhoneVerificationDispatchResult(
        long expiresInSeconds,
        long resendAfterSeconds
) {
}
