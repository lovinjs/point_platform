package com.core.coreboot.platform.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

public enum RefundMethod implements CodeEnum {
    ORIGINAL_CHANNEL("ORIGINAL_CHANNEL"),
    BANK_TRANSFER("BANK_TRANSFER"),
    OTHER("OTHER");

    @EnumValue
    @JsonValue
    private final String code;

    RefundMethod(String code) {
        this.code = code;
    }

    @Override
    public String getCode() {
        return code;
    }
}
