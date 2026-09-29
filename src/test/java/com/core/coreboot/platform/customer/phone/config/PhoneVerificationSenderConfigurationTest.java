package com.core.coreboot.platform.customer.phone.config;

import com.core.coreboot.exception.CustomException;
import com.core.coreboot.platform.customer.phone.service.AliyunPhoneVerificationSender;
import com.core.coreboot.platform.customer.phone.service.PhoneVerificationSender;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PhoneVerificationSenderConfigurationTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(PropertiesConfiguration.class, PhoneVerificationSenderConfiguration.class)
            .withBean(ObjectMapper.class, ObjectMapper::new);

    @Test
    void shouldRemainDisabledByDefault() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(PhoneVerificationSender.class);
            assertEquals(30042, assertThrows(CustomException.class,
                    context.getBean(PhoneVerificationSender.class)::validateReady).getCode());
        });
    }

    @Test
    void shouldKeepLogModeAsSingleSenderForLocalDevelopment() {
        contextRunner.withPropertyValues("platform.phone-verification.mode=LOG").run(context -> {
            assertThat(context).hasSingleBean(PhoneVerificationSender.class);
            assertThat(context).hasBean("loggingPhoneVerificationSender");
            assertThat(context).doesNotHaveBean("disabledPhoneVerificationSender");
            context.getBean(PhoneVerificationSender.class).validateReady();
        });
    }

    @Test
    void shouldBindAliyunSettingsAndSelectOnlyAliyunSender() {
        contextRunner.withPropertyValues(
                "platform.phone-verification.mode=ALIYUN",
                "platform.phone-verification.aliyun.access-key-id=test-id",
                "platform.phone-verification.aliyun.access-key-secret=test-secret",
                "platform.phone-verification.aliyun.sign-name=示例公司",
                "platform.phone-verification.aliyun.template-code=SMS_123456"
        ).run(context -> {
            assertThat(context).hasSingleBean(PhoneVerificationSender.class);
            assertThat(context).doesNotHaveBean("disabledPhoneVerificationSender");
            assertThat(context).doesNotHaveBean("loggingPhoneVerificationSender");
            assertThat(context.getBean(PhoneVerificationSender.class))
                    .isInstanceOf(AliyunPhoneVerificationSender.class);
            context.getBean(PhoneVerificationSender.class).validateReady();
            assertEquals(PhoneVerificationMode.ALIYUN,
                    context.getBean(PhoneVerificationProperties.class).getMode());
        });
    }

    @Test
    void shouldRejectMissingAliyunCredentialsWithoutFallingBackToLog() {
        contextRunner.withPropertyValues("platform.phone-verification.mode=ALIYUN").run(context -> {
            assertThat(context).hasSingleBean(PhoneVerificationSender.class);
            assertThat(context).doesNotHaveBean("loggingPhoneVerificationSender");
            assertEquals(30042, assertThrows(CustomException.class,
                    context.getBean(PhoneVerificationSender.class)::validateReady).getCode());
        });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties({PhoneVerificationProperties.class, AliyunSmsProperties.class})
    static class PropertiesConfiguration {
    }
}
