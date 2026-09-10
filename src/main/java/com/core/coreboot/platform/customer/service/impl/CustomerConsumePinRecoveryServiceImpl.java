package com.core.coreboot.platform.customer.service.impl;

import com.core.coreboot.exception.CustomException;
import com.core.coreboot.exception.ExceptionEnum;
import com.core.coreboot.platform.customer.config.CustomerPinProperties;
import com.core.coreboot.platform.customer.model.ConsumePinResetTokenResult;
import com.core.coreboot.platform.customer.phone.model.PhoneVerificationDispatchResult;
import com.core.coreboot.platform.customer.phone.service.CustomerPhoneService;
import com.core.coreboot.platform.customer.service.ConsumePinResetTokenStore;
import com.core.coreboot.platform.customer.service.CustomerConsumePinRecoveryService;
import com.core.coreboot.platform.customer.service.CustomerConsumePinService;
import com.core.coreboot.platform.customer.service.support.ConsumePinPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class CustomerConsumePinRecoveryServiceImpl implements CustomerConsumePinRecoveryService {
    private static final Duration MAXIMUM_RESET_TOKEN_TTL = Duration.ofMinutes(10);

    private final CustomerPhoneService customerPhoneService;
    private final ConsumePinResetTokenStore resetTokenStore;
    private final CustomerConsumePinService consumePinService;
    private final CustomerPinProperties properties;

    @Override
    public PhoneVerificationDispatchResult requestVerificationCode(Long customerId) {
        requireConfiguredPin(customerId);
        return customerPhoneService.requestBoundPhoneVerificationCode(customerId);
    }

    @Override
    public ConsumePinResetTokenResult verifyAndIssueResetToken(Long customerId, String verificationCode) {
        requireConfiguredPin(customerId);
        customerPhoneService.verifyBoundPhoneVerificationCode(customerId, verificationCode);
        Duration ttl = validatedResetTokenTtl();
        return new ConsumePinResetTokenResult(resetTokenStore.issue(customerId, ttl), ttl.toSeconds());
    }

    @Override
    public void resetPin(Long customerId, String resetToken, String newPin, String clientIp) {
        ConsumePinPolicy.requireStrong(newPin);
        consumePinService.resetPin(customerId, resetToken, newPin, clientIp);
    }

    private void requireConfiguredPin(Long customerId) {
        if (!consumePinService.getStatus(customerId).configured()) {
            throw new CustomException(ExceptionEnum.PLATFORM_CONSUME_PIN_NOT_SET);
        }
    }

    private Duration validatedResetTokenTtl() {
        Duration ttl = properties.getResetTokenTtl();
        if (ttl == null || ttl.compareTo(Duration.ofMinutes(1)) < 0 || ttl.compareTo(MAXIMUM_RESET_TOKEN_TTL) > 0) {
            throw new CustomException(ExceptionEnum.PLATFORM_CUSTOMER_PIN_CONFIGURATION_INVALID);
        }
        return ttl;
    }
}
