package com.core.coreboot.platform.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

public enum PointLedgerBusinessType implements CodeEnum {
    RECHARGE_ORDER("RECHARGE_ORDER"),
    CONSUMPTION_ORDER("CONSUMPTION_ORDER"),
    RECHARGE_REFUND("RECHARGE_REFUND"),
    MANUAL_ADJUSTMENT("MANUAL_ADJUSTMENT");

    @EnumValue
    @JsonValue
    private final String code;

    PointLedgerBusinessType(String code) {
        this.code = code;
    }

    @Override
    public String getCode() {
        return code;
    }
}
