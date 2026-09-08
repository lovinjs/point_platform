package com.core.coreboot.platform.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

public enum SettlementPeriodStatus implements CodeEnum {
    OPEN("OPEN"),
    FROZEN("FROZEN"),
    GENERATED("GENERATED"),
    CONFIRMED("CONFIRMED"),
    PAID("PAID"),
    CLOSED("CLOSED");

    @EnumValue
    @JsonValue
    private final String code;

    SettlementPeriodStatus(String code) {
        this.code = code;
    }

    @Override
    public String getCode() {
        return code;
    }
}
