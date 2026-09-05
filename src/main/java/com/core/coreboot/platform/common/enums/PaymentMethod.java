package com.core.coreboot.platform.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

public enum PaymentMethod implements CodeEnum {
    PLATFORM_QR("PLATFORM_QR"),
    BANK_TRANSFER("BANK_TRANSFER"),
    OTHER("OTHER");

    @EnumValue
    @JsonValue
    private final String code;

    PaymentMethod(String code) {
        this.code = code;
    }

    @Override
    public String getCode() {
        return code;
    }
}
