package com.core.coreboot.platform.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

public enum RoleCode implements CodeEnum {
    CLERK("CLERK"),
    STORE_MANAGER("STORE_MANAGER"),
    SUPER_ADMIN("SUPER_ADMIN");

    @EnumValue
    @JsonValue
    private final String code;

    RoleCode(String code) {
        this.code = code;
    }

    @Override
    public String getCode() {
        return code;
    }
}
