package com.core.coreboot.platform.auth.service;

import com.core.coreboot.platform.auth.model.AdminLoginRequest;
import com.core.coreboot.platform.auth.model.AdminLoginResponse;

public interface AdminAuthenticationService {
    AdminLoginResponse login(AdminLoginRequest request, String clientIp);
}
