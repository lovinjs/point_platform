package com.core.coreboot.platform.customer.auth.config;

import com.core.coreboot.exception.CustomException;
import com.core.coreboot.exception.ExceptionEnum;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WechatH5SettingsProviderTest {

    @Test
    void shouldAcceptLocalHttpUrlsForDevelopment() {
        WechatH5AuthProperties properties = validProperties();

        WechatH5Settings settings = new WechatH5SettingsProvider(properties).requireValid();

        assertEquals("wx-test-app", settings.appId());
        assertEquals("http://192.168.1.220:5173/api/v1/h5/auth/wechat/callback", settings.callbackUrl());
        assertEquals("http://192.168.1.220:5173", settings.h5BaseUrl());
        assertEquals("snsapi_userinfo", settings.scope());
    }

    @Test
    void shouldRejectDisabledWechatLogin() {
        WechatH5AuthProperties properties = validProperties();
        properties.setEnabled(false);

        CustomException exception = assertThrows(
                CustomException.class,
                () -> new WechatH5SettingsProvider(properties).requireValid()
        );

        assertEquals(ExceptionEnum.PLATFORM_WECHAT_AUTH_NOT_CONFIGURED.getCode(), exception.getCode());
    }

    @Test
    void shouldRejectCallbackUrlWithUnexpectedPath() {
        WechatH5AuthProperties properties = validProperties();
        properties.setCallbackUrl("http://192.168.1.220:5173/wrong-callback");

        CustomException exception = assertThrows(
                CustomException.class,
                () -> new WechatH5SettingsProvider(properties).requireValid()
        );

        assertEquals(ExceptionEnum.PLATFORM_WECHAT_AUTH_NOT_CONFIGURED.getCode(), exception.getCode());
    }

    private WechatH5AuthProperties validProperties() {
        WechatH5AuthProperties properties = new WechatH5AuthProperties();
        properties.setEnabled(true);
        properties.setAppId("wx-test-app");
        properties.setAppSecret("test-secret");
        properties.setCallbackUrl("http://192.168.1.220:5173/api/v1/h5/auth/wechat/callback");
        properties.setH5BaseUrl("http://192.168.1.220:5173/");
        properties.setScope("snsapi_userinfo");
        properties.setStateTtl(Duration.ofMinutes(5));
        properties.setConnectTimeout(Duration.ofSeconds(5));
        properties.setReadTimeout(Duration.ofSeconds(8));
        return properties;
    }
}
