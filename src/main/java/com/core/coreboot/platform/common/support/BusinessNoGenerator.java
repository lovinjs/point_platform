package com.core.coreboot.platform.common.support;

import java.util.Locale;
import java.util.UUID;

/**
 * Generates collision-resistant business numbers without sharing the weak legacy order-number generator.
 */
public final class BusinessNoGenerator {
    private BusinessNoGenerator() {
    }

    public static String next(String prefix) {
        if (prefix == null || prefix.isBlank() || prefix.length() > 8) {
            throw new IllegalArgumentException("业务编号前缀长度必须在1到8之间");
        }
        String normalizedPrefix = prefix.trim().toUpperCase(Locale.ROOT);
        String randomPart = UUID.randomUUID().toString().replace("-", "").toUpperCase(Locale.ROOT);
        return normalizedPrefix + randomPart;
    }
}
