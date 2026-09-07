package com.core.coreboot.platform.customer.phone.service;

import com.core.coreboot.platform.customer.auth.model.CustomerAccountView;
import com.core.coreboot.platform.customer.phone.model.PhoneVerificationDispatchResult;

public interface CustomerPhoneService {
    PhoneVerificationDispatchResult requestVerificationCode(Long customerId, String phone);

    CustomerAccountView bindPhone(
            Long customerId,
            String phone,
            String verificationCode,
            String clientIp
    );
}
