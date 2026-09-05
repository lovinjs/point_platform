package com.core.coreboot.platform.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

public enum PointLotStatus implements CodeEnum {
    AVAILABLE("AVAILABLE"),
    DEPLETED("DEPLETED"),
    REFUNDED("REFUNDED");

    @EnumValue
    @JsonValue
    private final String code;

    PointLotStatus(String code) {
        this.code = code;
    }

    @Override
    public String getCode() {
        return code;
    }
}
