package com.core.coreboot.platform.customer.phone.service;

import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.SendSmsRequest;
import com.aliyun.dysmsapi20170525.models.SendSmsResponse;
import com.aliyun.dysmsapi20170525.models.SendSmsResponseBody;
import com.aliyun.tea.TeaException;
import com.aliyun.teautil.models.RuntimeOptions;
import com.core.coreboot.exception.CustomException;
import com.core.coreboot.exception.ExceptionEnum;
import com.core.coreboot.platform.customer.phone.config.AliyunSmsProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AliyunPhoneVerificationSenderTest {
    private static final String PHONE = "13800138000";
    private static final Duration CODE_TTL = Duration.ofMinutes(5);

    @Mock
    private Client client;

    private AliyunSmsProperties properties;
    private ObjectMapper objectMapper;
    private AliyunPhoneVerificationSender sender;

    @BeforeEach
    void setUp() {
        properties = new AliyunSmsProperties();
        properties.setAccessKeyId("test-access-key-id");
        properties.setAccessKeySecret("test-access-key-secret");
        properties.setSignName("示例公司");
        properties.setTemplateCode("SMS_123456");
        objectMapper = new ObjectMapper();
        sender = new AliyunPhoneVerificationSender(properties, objectMapper, client);
    }

    @Test
    void shouldSendOnlyCodeVariableAndPreserveLeadingZerosWithoutRetries() throws Exception {
        when(client.sendSmsWithOptions(any(), any())).thenReturn(response("OK"));

        sender.send(PHONE, "000123", CODE_TTL);

        ArgumentCaptor<SendSmsRequest> request = ArgumentCaptor.forClass(SendSmsRequest.class);
        ArgumentCaptor<RuntimeOptions> runtime = ArgumentCaptor.forClass(RuntimeOptions.class);
        verify(client).sendSmsWithOptions(request.capture(), runtime.capture());
        assertEquals(PHONE, request.getValue().getPhoneNumbers());
        assertEquals("示例公司", request.getValue().getSignName());
        assertEquals("SMS_123456", request.getValue().getTemplateCode());
        assertEquals(Map.of("code", "000123"),
                objectMapper.readValue(request.getValue().getTemplateParam(), Map.class));
        assertFalse(runtime.getValue().getAutoretry());
        assertEquals(1, runtime.getValue().getMaxAttempts());
    }

    @ParameterizedTest
    @ValueSource(strings = {"isv.AMOUNT_NOT_ENOUGH", "isv.BUSINESS_LIMIT_CONTROL",
            "isv.SMS_SIGNATURE_ILLEGAL", "isv.SMS_TEMPLATE_ILLEGAL", ""})
    void shouldTreatProviderRejectionAsSendFailure(String code) throws Exception {
        when(client.sendSmsWithOptions(any(), any())).thenReturn(response(code));

        CustomException exception = assertThrows(CustomException.class,
                () -> sender.send(PHONE, "825391", CODE_TTL));

        assertEquals(ExceptionEnum.PLATFORM_SMS_SEND_FAILED.getCode(), exception.getCode());
        assertEquals(ExceptionEnum.PLATFORM_SMS_SEND_FAILED.getMsg(), exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void shouldRejectEmptyResponse() throws Exception {
        when(client.sendSmsWithOptions(any(), any())).thenReturn(null);

        assertEquals(30050, assertThrows(CustomException.class,
                () -> sender.send(PHONE, "825391", CODE_TTL)).getCode());
    }

    @Test
    void shouldRejectEmptyResponseBody() throws Exception {
        when(client.sendSmsWithOptions(any(), any())).thenReturn(new SendSmsResponse());

        assertEquals(30050, assertThrows(CustomException.class,
                () -> sender.send(PHONE, "825391", CODE_TTL)).getCode());
    }

    @Test
    void shouldNotRetryOrExposeSdkExceptionAfterTimeout() throws Exception {
        when(client.sendSmsWithOptions(any(), any()))
                .thenThrow(new SocketTimeoutException("sensitive-request-parameters"));

        CustomException exception = assertThrows(CustomException.class,
                () -> sender.send(PHONE, "825391", CODE_TTL));

        assertEquals(30050, exception.getCode());
        assertEquals(ExceptionEnum.PLATFORM_SMS_SEND_FAILED.getMsg(), exception.getMessage());
        assertNull(exception.getCause());
        verify(client, times(1)).sendSmsWithOptions(any(), any());
    }

    @Test
    void shouldTranslateRamPermissionFailureWithoutExposingProviderMessage() throws Exception {
        when(client.sendSmsWithOptions(any(), any())).thenThrow(new TeaException(Map.of(
                "code", "Forbidden.RAM",
                "message", "sensitive-request-parameters"
        )));

        CustomException exception = assertThrows(CustomException.class,
                () -> sender.send(PHONE, "825391", CODE_TTL));

        assertEquals(30050, exception.getCode());
        assertEquals(ExceptionEnum.PLATFORM_SMS_SEND_FAILED.getMsg(), exception.getMessage());
        assertNull(exception.getCause());
    }

    @ParameterizedTest
    @MethodSource("invalidConfigurations")
    void shouldFailBeforeCallingSdkWhenConfigurationIsIncomplete(Consumer<AliyunSmsProperties> change) {
        change.accept(properties);

        assertEquals(30042, assertThrows(CustomException.class, sender::validateReady).getCode());
        assertEquals(30042, assertThrows(CustomException.class,
                () -> sender.send(PHONE, "825391", CODE_TTL)).getCode());
        verifyNoInteractions(client);
    }

    @ParameterizedTest
    @ValueSource(strings = {"13800138000,13900139000", "invalid", ""})
    void shouldNeverSendToMultipleOrInvalidPhoneNumbers(String phone) {
        assertThrows(CustomException.class, () -> sender.send(phone, "825391", CODE_TTL));
        verifyNoInteractions(client);
    }

    @ParameterizedTest
    @ValueSource(strings = {"12345", "1234567", "abcdef", ""})
    void shouldRejectInvalidCodeBeforeCallingSdk(String code) {
        assertThrows(CustomException.class, () -> sender.send(PHONE, code, CODE_TTL));
        verifyNoInteractions(client);
    }

    private static Stream<Consumer<AliyunSmsProperties>> invalidConfigurations() {
        return Stream.of(
                properties -> properties.setAccessKeyId(null),
                properties -> properties.setAccessKeyId(" "),
                properties -> properties.setAccessKeySecret(null),
                properties -> properties.setAccessKeySecret(" "),
                properties -> properties.setSignName(""),
                properties -> properties.setTemplateCode(" "),
                properties -> properties.setConnectTimeout(null),
                properties -> properties.setConnectTimeout(Duration.ofMillis(999)),
                properties -> properties.setReadTimeout(Duration.ofSeconds(31))
        );
    }

    private SendSmsResponse response(String code) {
        return new SendSmsResponse().setBody(new SendSmsResponseBody()
                .setCode(code).setRequestId("test-request-id").setMessage("provider-message"));
    }
}
