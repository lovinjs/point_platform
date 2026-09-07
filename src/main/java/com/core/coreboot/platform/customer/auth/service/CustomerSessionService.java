package com.core.coreboot.platform.customer.auth.service;

import com.core.coreboot.platform.customer.auth.model.CustomerSessionResponse;

public interface CustomerSessionService {
    CustomerSessionResponse exchangeLoginTicket(String ticket);
}
