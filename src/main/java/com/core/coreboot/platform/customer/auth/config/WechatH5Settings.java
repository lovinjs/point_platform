package com.core.coreboot.platform.customer.auth.config;

import java.time.Duration;

public record WechatH5Settings(
        String appId,
        String appSecret,
        String callbackUrl,
        String h5BaseUrl,
        String scope,
        Duration stateTtl,
        Duration connectTimeout,
        Duration readTimeout
) {
}
