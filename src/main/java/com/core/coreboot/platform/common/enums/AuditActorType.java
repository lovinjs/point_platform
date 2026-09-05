package com.core.coreboot.platform.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

public enum AuditActorType implements CodeEnum {
    CUSTOMER("CUSTOMER"),
    SYS_USER("SYS_USER"),
    SYSTEM("SYSTEM");

    @EnumValue
    @JsonValue
    private final String code;

    AuditActorType(String code) {
        this.code = code;
    }

    @Override
    public String getCode() {
        return code;
    }
}
