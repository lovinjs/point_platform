package com.core.coreboot.platform.customer.service;

import com.core.coreboot.platform.customer.model.ConsumePinStatus;

public interface CustomerConsumePinService {
    ConsumePinStatus getStatus(Long customerId);

    void setInitialPin(Long customerId, String newPin, String clientIp);

    void changePin(Long customerId, String currentPin, String newPin, String clientIp);

    void resetPin(Long customerId, String resetToken, String newPin, String clientIp);

    void verifyPin(Long customerId, String pin, String clientIp);
}
