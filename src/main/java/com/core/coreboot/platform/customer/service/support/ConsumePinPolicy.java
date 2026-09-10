package com.core.coreboot.platform.customer.service.support;

import com.core.coreboot.exception.CustomException;
import com.core.coreboot.exception.ExceptionEnum;

import java.util.Set;
import java.util.regex.Pattern;

public final class ConsumePinPolicy {
    private static final Pattern PIN_PATTERN = Pattern.compile("\\d{6}");
    private static final Set<String> WEAK_PINS = Set.of(
            "000000", "111111", "222222", "333333", "444444",
            "555555", "666666", "777777", "888888", "999999",
            "012345", "123456", "234567", "345678", "456789",
            "987654", "876543", "765432", "654321", "543210"
    );

    private ConsumePinPolicy() {
    }

    public static void requireStrong(String pin) {
        if (pin == null || !PIN_PATTERN.matcher(pin).matches() || WEAK_PINS.contains(pin)) {
            throw new CustomException(ExceptionEnum.PLATFORM_CONSUME_PIN_FORMAT_INVALID);
        }
    }

    public static boolean isFormatValid(String pin) {
        return pin != null && PIN_PATTERN.matcher(pin).matches();
    }
}
