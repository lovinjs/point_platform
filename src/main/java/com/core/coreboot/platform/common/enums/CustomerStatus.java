package com.core.coreboot.platform.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

public enum CustomerStatus implements CodeEnum {
    ACTIVE("ACTIVE"),
    DISABLED("DISABLED");

    @EnumValue
    @JsonValue
    private final String code;

    CustomerStatus(String code) {
        this.code = code;
    }

    @Override
    public String getCode() {
        return code;
    }
}
