package com.core.coreboot.platform.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

public enum FundReceiver implements CodeEnum {
    PLATFORM("PLATFORM");

    @EnumValue
    @JsonValue
    private final String code;

    FundReceiver(String code) {
        this.code = code;
    }

    @Override
    public String getCode() {
        return code;
    }
}
