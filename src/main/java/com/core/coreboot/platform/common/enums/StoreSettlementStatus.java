package com.core.coreboot.platform.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

public enum StoreSettlementStatus implements CodeEnum {
    GENERATED("GENERATED"),
    CONFIRMED("CONFIRMED"),
    PAID("PAID"),
    CLOSED("CLOSED");

    @EnumValue
    @JsonValue
    private final String code;

    StoreSettlementStatus(String code) {
        this.code = code;
    }

    @Override
    public String getCode() {
        return code;
    }
}
