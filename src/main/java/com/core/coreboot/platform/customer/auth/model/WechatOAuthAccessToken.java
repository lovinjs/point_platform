package com.core.coreboot.platform.customer.auth.model;

public record WechatOAuthAccessToken(
        String accessToken,
        String openId,
        String scope,
        String unionId
) {
}
