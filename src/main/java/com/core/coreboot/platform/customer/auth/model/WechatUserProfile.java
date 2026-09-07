package com.core.coreboot.platform.customer.auth.model;

public record WechatUserProfile(
        String openId,
        String nickname,
        String avatarUrl,
        String unionId
) {
}
