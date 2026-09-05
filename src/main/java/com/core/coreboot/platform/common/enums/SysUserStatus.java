package com.core.coreboot.platform.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

public enum SysUserStatus implements CodeEnum {
    ACTIVE("ACTIVE"),
    DISABLED("DISABLED"),
    LOCKED("LOCKED");

    @EnumValue
    @JsonValue
    private final String code;

    SysUserStatus(String code) {
        this.code = code;
    }

    @Override
    public String getCode() {
        return code;
    }
}
