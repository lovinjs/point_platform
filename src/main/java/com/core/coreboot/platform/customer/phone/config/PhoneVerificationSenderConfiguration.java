package com.core.coreboot.platform.customer.phone.config;

import com.core.coreboot.exception.CustomException;
import com.core.coreboot.exception.ExceptionEnum;
import com.core.coreboot.platform.customer.phone.service.PhoneVerificationSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class PhoneVerificationSenderConfiguration {

    @Bean
    @ConditionalOnProperty(
            prefix = "platform.phone-verification",
            name = "mode",
            havingValue = "LOG"
    )
    public PhoneVerificationSender loggingPhoneVerificationSender() {
        return new LoggingPhoneVerificationSender();
    }

    @Bean
    @ConditionalOnMissingBean(PhoneVerificationSender.class)
    public PhoneVerificationSender disabledPhoneVerificationSender() {
        return new DisabledPhoneVerificationSender();
    }

    private static final class LoggingPhoneVerificationSender implements PhoneVerificationSender {
        private static final Logger log = LoggerFactory.getLogger(LoggingPhoneVerificationSender.class);

        @Override
        public void validateReady() {
            // LOG 模式由环境变量显式开启，仅供本地联调。
        }

        @Override
        public void send(String phone, String verificationCode, Duration expiresIn) {
            log.warn(
                    "本地联调短信验证码={}，手机号={}，有效期={}秒；正式环境禁止使用 LOG 模式",
                    verificationCode,
                    maskPhone(phone),
                    expiresIn.toSeconds()
            );
        }

        private String maskPhone(String phone) {
            return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
        }
    }

    private static final class DisabledPhoneVerificationSender implements PhoneVerificationSender {
        @Override
        public void validateReady() {
            throw new CustomException(ExceptionEnum.PLATFORM_PHONE_VERIFICATION_NOT_CONFIGURED);
        }

        @Override
        public void send(String phone, String verificationCode, Duration expiresIn) {
            throw new CustomException(ExceptionEnum.PLATFORM_PHONE_VERIFICATION_NOT_CONFIGURED);
        }
    }
}
