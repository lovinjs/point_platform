package com.core.coreboot.platform.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

public enum StoreStatus implements CodeEnum {
    PENDING("PENDING"),
    ACTIVE("ACTIVE"),
    SUSPENDED("SUSPENDED"),
    CLOSED("CLOSED");

    @EnumValue
    @JsonValue
    private final String code;

    StoreStatus(String code) {
        this.code = code;
    }

    @Override
    public String getCode() {
        return code;
    }
}
