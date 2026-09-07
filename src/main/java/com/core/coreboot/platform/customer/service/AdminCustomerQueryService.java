package com.core.coreboot.platform.customer.service;

import com.core.coreboot.platform.customer.model.AdminCustomerView;

public interface AdminCustomerQueryService {
    AdminCustomerView findByPhone(String phone);
}
