package com.core.coreboot.platform.customer.auth.service;

import com.core.coreboot.platform.customer.auth.model.CustomerLoginTicket;

public interface CustomerLoginTicketStore {
    String issue(Long customerId, String returnPath);

    CustomerLoginTicket consume(String ticket);
}
