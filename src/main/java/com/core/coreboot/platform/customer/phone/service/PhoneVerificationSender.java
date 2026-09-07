package com.core.coreboot.platform.customer.phone.service;

import java.time.Duration;

public interface PhoneVerificationSender {
    void validateReady();

    void send(String phone, String verificationCode, Duration expiresIn);
}
