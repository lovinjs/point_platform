package com.core.coreboot.platform.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

public enum MerchantStatus implements CodeEnum {
    PENDING("PENDING"),
    ACTIVE("ACTIVE"),
    SUSPENDED("SUSPENDED"),
    TERMINATED("TERMINATED");

    @EnumValue
    @JsonValue
    private final String code;

    MerchantStatus(String code) {
        this.code = code;
    }

    @Override
    public String getCode() {
        return code;
    }
}
