package com.core.coreboot.platform.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

public enum RechargeOrderStatus implements CodeEnum {
    CREATED("CREATED"),
    COMPLETED("COMPLETED"),
    REFUNDED("REFUNDED"),
    CANCELLED("CANCELLED");

    @EnumValue
    @JsonValue
    private final String code;

    RechargeOrderStatus(String code) {
        this.code = code;
    }

    @Override
    public String getCode() {
        return code;
    }
}
