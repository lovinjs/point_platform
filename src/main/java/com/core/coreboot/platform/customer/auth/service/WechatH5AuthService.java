package com.core.coreboot.platform.customer.auth.service;

import java.net.URI;

public interface WechatH5AuthService {
    URI createAuthorizationRedirect(String returnPath);

    URI completeAuthorization(String code, String state);
}
