package com.core.coreboot.platform.customer.auth.service;

import com.core.coreboot.exception.CustomException;
import com.core.coreboot.exception.ExceptionEnum;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WechatH5ApiClientTest {
    private final WechatH5ApiClient client = new WechatH5ApiClient(new ObjectMapper());

    @Test
    void shouldDecodeWechatJsonFromRawUtf8BytesWithoutDependingOnContentType() {
        byte[] responseBody = "{\"nickname\":\"微信用户\"}".getBytes(StandardCharsets.UTF_8);

        TestResponse response = client.readJson("test", responseBody, TestResponse.class);

        assertEquals("微信用户", response.nickname());
    }

    @Test
    void shouldRejectNonJsonWechatResponseWithoutLoggingItsContent() {
        byte[] responseBody = "not-json".getBytes(StandardCharsets.UTF_8);

        CustomException exception = assertThrows(
                CustomException.class,
                () -> client.readJson("test", responseBody, TestResponse.class)
        );

        assertEquals(ExceptionEnum.PLATFORM_WECHAT_API_UNAVAILABLE.getCode(), exception.getCode());
    }

    private record TestResponse(String nickname) {
    }
}
