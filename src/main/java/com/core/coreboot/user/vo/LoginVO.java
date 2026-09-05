package com.core.coreboot.user.vo;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Data
@Builder
public class LoginVO implements Serializable {
    private String token;

    private String tokenType;

    private UserVO userInfo;
}
