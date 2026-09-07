package com.core.coreboot.platform.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

public enum SettlementStatus implements CodeEnum {
    NOT_INCLUDED("NOT_INCLUDED"),
    INCLUDED("INCLUDED"),
    SETTLED("SETTLED"),
    ADJUSTED("ADJUSTED");

    @EnumValue
    @JsonValue
    private final String code;

    SettlementStatus(String code) {
        this.code = code;
    }

    @Override
    public String getCode() {
        return code;
    }
}
