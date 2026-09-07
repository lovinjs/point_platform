package com.core.coreboot.platform.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ConsumptionVerificationMode implements CodeEnum {
    CUSTOMER_PIN("CUSTOMER_PIN");

    @EnumValue
    @JsonValue
    private final String code;

    ConsumptionVerificationMode(String code) {
        this.code = code;
    }

    @Override
    public String getCode() {
        return code;
    }
}
