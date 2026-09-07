package com.core.coreboot.platform.customer.auth.service;

import com.core.coreboot.platform.customer.auth.model.WechatOAuthState;

public interface WechatOAuthStateStore {
    String issue(String returnPath);

    WechatOAuthState consume(String state);
}
