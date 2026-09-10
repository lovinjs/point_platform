package com.core.coreboot.platform.customer.service;

import com.core.coreboot.platform.customer.model.ConsumePinResetTokenResult;
import com.core.coreboot.platform.customer.phone.model.PhoneVerificationDispatchResult;

public interface CustomerConsumePinRecoveryService {
    PhoneVerificationDispatchResult requestVerificationCode(Long customerId);

    ConsumePinResetTokenResult verifyAndIssueResetToken(Long customerId, String verificationCode);

    void resetPin(Long customerId, String resetToken, String newPin, String clientIp);
}
