package com.core.coreboot.platform.customer.phone.service;

import com.core.coreboot.platform.customer.phone.config.PhoneVerificationProperties;

import java.time.Duration;

public interface PhoneVerificationCodeStore {
    void reserveSend(Long customerId, String phone, PhoneVerificationProperties properties);

    void saveCode(Long customerId, String phone, String verificationCode, Duration ttl);

    void verifyAndConsume(Long customerId, String phone, String verificationCode, int maxAttempts);

    void invalidate(Long customerId, String phone);
}
