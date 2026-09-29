package com.core.coreboot.platform.customer.phone.service;

import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.SendSmsRequest;
import com.aliyun.dysmsapi20170525.models.SendSmsResponse;
import com.aliyun.tea.TeaException;
import com.aliyun.teaopenapi.models.Config;
import com.aliyun.teautil.models.RuntimeOptions;
import com.core.coreboot.exception.CustomException;
import com.core.coreboot.exception.ExceptionEnum;
import com.core.coreboot.platform.customer.phone.config.AliyunSmsProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Map;
import java.util.regex.Pattern;

public class AliyunPhoneVerificationSender implements PhoneVerificationSender {
    private static final Logger log = LoggerFactory.getLogger(AliyunPhoneVerificationSender.class);
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");
    private static final Pattern CODE_PATTERN = Pattern.compile("^\\d{6}$");

    private final AliyunSmsProperties properties;
    private final ObjectMapper objectMapper;
    private volatile Client client;

    public AliyunPhoneVerificationSender(AliyunSmsProperties properties, ObjectMapper objectMapper) {
        this(properties, objectMapper, null);
    }

    AliyunPhoneVerificationSender(AliyunSmsProperties properties, ObjectMapper objectMapper, Client client) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.client = client;
    }

    @Override
    public void validateReady() {
        if (isBlank(properties.getAccessKeyId())
                || isBlank(properties.getAccessKeySecret())
                || isBlank(properties.getSignName())
                || isBlank(properties.getTemplateCode())
                || invalidTimeout(properties.getConnectTimeout())
                || invalidTimeout(properties.getReadTimeout())) {
            throw new CustomException(ExceptionEnum.PLATFORM_PHONE_VERIFICATION_NOT_CONFIGURED);
        }
    }

    @Override
    public void send(String phone, String verificationCode, Duration expiresIn) {
        validateReady();
        if (phone == null || !PHONE_PATTERN.matcher(phone).matches()
                || verificationCode == null || !CODE_PATTERN.matcher(verificationCode).matches()) {
            throw new CustomException(ExceptionEnum.PLATFORM_INVALID_REQUEST);
        }

        SendSmsResponse response;
        try {
            SendSmsRequest request = new SendSmsRequest()
                    .setPhoneNumbers(phone)
                    .setSignName(properties.getSignName().trim())
                    .setTemplateCode(properties.getTemplateCode().trim())
                    // 当前审核模板只有 code 变量；有效期由验证码存储层控制。
                    .setTemplateParam(objectMapper.writeValueAsString(Map.of("code", verificationCode)));
            RuntimeOptions runtime = new RuntimeOptions()
                    // SendSms 不支持幂等，网络超时后不能自动重复提交。
                    .setAutoretry(false)
                    .setMaxAttempts(1);
            response = requireClient().sendSmsWithOptions(request, runtime);
        } catch (Exception ex) {
            // SDK 异常可能携带请求参数，禁止打印原始异常消息和堆栈。
            if (ex instanceof TeaException sdkException) {
                Object requestId = sdkException.getData() == null
                        ? null : sdkException.getData().get("RequestId");
                log.warn("阿里云短信调用失败，code={}，requestId={}", sdkException.getCode(), requestId);
            } else {
                log.warn("阿里云短信调用失败，异常类型={}", ex.getClass().getSimpleName());
            }
            throw new CustomException(ExceptionEnum.PLATFORM_SMS_SEND_FAILED);
        }

        if (response == null || response.getBody() == null) {
            log.warn("阿里云短信调用返回空响应");
            throw new CustomException(ExceptionEnum.PLATFORM_SMS_SEND_FAILED);
        }
        if (!"OK".equals(response.getBody().getCode())) {
            log.warn("阿里云短信提交失败，code={}，requestId={}",
                    response.getBody().getCode(), response.getBody().getRequestId());
            throw new CustomException(ExceptionEnum.PLATFORM_SMS_SEND_FAILED);
        }
    }

    private Client requireClient() throws Exception {
        Client current = client;
        if (current == null) {
            synchronized (this) {
                current = client;
                if (current == null) {
                    Config config = new Config()
                            .setAccessKeyId(properties.getAccessKeyId())
                            .setAccessKeySecret(properties.getAccessKeySecret())
                            .setEndpoint("dysmsapi.aliyuncs.com")
                            .setRegionId("cn-hangzhou")
                            .setProtocol("https")
                            .setConnectTimeout(Math.toIntExact(properties.getConnectTimeout().toMillis()))
                            .setReadTimeout(Math.toIntExact(properties.getReadTimeout().toMillis()));
                    current = new Client(config);
                    client = current;
                }
            }
        }
        return current;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private boolean invalidTimeout(Duration value) {
        return value == null
                || value.compareTo(Duration.ofSeconds(1)) < 0
                || value.compareTo(Duration.ofSeconds(30)) > 0;
    }
}
