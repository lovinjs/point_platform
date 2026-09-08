package com.core.coreboot.platform.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

public enum SettlementItemType implements CodeEnum {
    CONSUMPTION("CONSUMPTION"),
    REVERSAL_ADJUSTMENT("REVERSAL_ADJUSTMENT"),
    MANUAL_ADJUSTMENT("MANUAL_ADJUSTMENT");

    @EnumValue
    @JsonValue
    private final String code;

    SettlementItemType(String code) {
        this.code = code;
    }

    @Override
    public String getCode() {
        return code;
    }
}
