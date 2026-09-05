package com.core.coreboot.user.vo;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class UserVO {
    private Integer id;

    private String name;

    private String signature;

    private Integer role;
}