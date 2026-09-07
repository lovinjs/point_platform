package com.core.coreboot.platform.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ConsumptionOrderStatus implements CodeEnum {
    PENDING_CONFIRM("PENDING_CONFIRM"),
    COMPLETED("COMPLETED"),
    CANCELLED("CANCELLED"),
    EXPIRED("EXPIRED"),
    REVERSED("REVERSED");

    @EnumValue
    @JsonValue
    private final String code;

    ConsumptionOrderStatus(String code) {
        this.code = code;
    }

    @Override
    public String getCode() {
        return code;
    }
}
