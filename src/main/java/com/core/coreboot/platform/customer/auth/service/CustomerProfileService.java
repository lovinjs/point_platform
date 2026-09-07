package com.core.coreboot.platform.customer.auth.service;

import com.core.coreboot.platform.customer.auth.model.CustomerAccountView;

public interface CustomerProfileService {
    CustomerAccountView getProfile(Long customerId);
}
