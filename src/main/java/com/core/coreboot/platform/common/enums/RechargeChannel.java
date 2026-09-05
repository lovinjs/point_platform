package com.core.coreboot.platform.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

public enum RechargeChannel implements CodeEnum {
    OFFLINE("OFFLINE"),
    WECHAT_PAY("WECHAT_PAY");

    @EnumValue
    @JsonValue
    private final String code;

    RechargeChannel(String code) {
        this.code = code;
    }

    @Override
    public String getCode() {
        return code;
    }
}
