package com.core.coreboot.platform.auth.service.impl;

import com.core.coreboot.exception.CustomException;
import com.core.coreboot.exception.ExceptionEnum;
import com.core.coreboot.platform.auth.model.AdminAccountView;
import com.core.coreboot.platform.auth.model.AdminLoginRequest;
import com.core.coreboot.platform.auth.model.AdminLoginResponse;
import com.core.coreboot.platform.auth.security.AdminTokenService;
import com.core.coreboot.platform.auth.security.AdminUserPrincipal;
import com.core.coreboot.platform.auth.service.AdminAuthenticationService;
import com.core.coreboot.platform.auth.service.AdminLoginAttemptRecorder;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminAuthenticationServiceImpl implements AdminAuthenticationService {
    private final AuthenticationManager authenticationManager;
    private final AdminTokenService adminTokenService;
    private final AdminLoginAttemptRecorder loginAttemptRecorder;

    @Override
    public AdminLoginResponse login(AdminLoginRequest request, String clientIp) {
        if (request == null
                || request.username() == null
                || request.username().isBlank()
                || request.password() == null
                || request.password().isBlank()) {
            throw new CustomException(ExceptionEnum.PLATFORM_ADMIN_LOGIN_FAILED);
        }
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(
                            request.username().trim(),
                            request.password()
                    )
            );
        } catch (AuthenticationException ex) {
            loginAttemptRecorder.recordFailure(request.username());
            throw new CustomException(ExceptionEnum.PLATFORM_ADMIN_LOGIN_FAILED);
        }

        if (!(authentication.getPrincipal() instanceof AdminUserPrincipal principal)) {
            throw new CustomException(ExceptionEnum.PLATFORM_ADMIN_LOGIN_FAILED);
        }

        AdminTokenService.IssuedAdminToken issuedToken = adminTokenService.issue(principal);
        loginAttemptRecorder.recordSuccess(principal, clientIp);

        return new AdminLoginResponse(
                issuedToken.token(),
                "Bearer",
                issuedToken.expiresInSeconds(),
                issuedToken.expiresAt(),
                AdminAccountView.from(principal)
        );
    }

}
