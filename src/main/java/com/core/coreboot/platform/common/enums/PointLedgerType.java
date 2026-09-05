package com.core.coreboot.platform.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

public enum PointLedgerType implements CodeEnum {
    RECHARGE("RECHARGE"),
    CONSUME("CONSUME"),
    REFUND("REFUND"),
    ADJUSTMENT("ADJUSTMENT"),
    REVERSAL("REVERSAL");

    @EnumValue
    @JsonValue
    private final String code;

    PointLedgerType(String code) {
        this.code = code;
    }

    @Override
    public String getCode() {
        return code;
    }
}
