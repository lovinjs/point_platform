package com.core.coreboot.platform.customer.service;

import java.time.Duration;

public interface ConsumePinResetTokenStore {
    String issue(Long customerId, Duration ttl);

    void consume(Long customerId, String resetToken);
}
