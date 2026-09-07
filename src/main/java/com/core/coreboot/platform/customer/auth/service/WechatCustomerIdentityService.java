package com.core.coreboot.platform.customer.auth.service;

import com.core.coreboot.platform.customer.auth.model.WechatUserProfile;

public interface WechatCustomerIdentityService {
    Long resolveCustomer(String appId, WechatUserProfile profile);
}
