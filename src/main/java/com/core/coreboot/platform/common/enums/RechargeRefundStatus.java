package com.core.coreboot.platform.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

public enum RechargeRefundStatus implements CodeEnum {
    CREATED("CREATED"),
    COMPLETED("COMPLETED"),
    CANCELLED("CANCELLED"),
    FAILED("FAILED");

    @EnumValue
    @JsonValue
    private final String code;

    RechargeRefundStatus(String code) {
        this.code = code;
    }

    @Override
    public String getCode() {
        return code;
    }
}
