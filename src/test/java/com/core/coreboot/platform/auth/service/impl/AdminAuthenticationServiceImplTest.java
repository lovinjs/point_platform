package com.core.coreboot.platform.auth.service.impl;

import com.core.coreboot.exception.CustomException;
import com.core.coreboot.exception.ExceptionEnum;
import com.core.coreboot.platform.auth.model.AdminLoginRequest;
import com.core.coreboot.platform.auth.model.AdminLoginResponse;
import com.core.coreboot.platform.auth.security.AdminTokenService;
import com.core.coreboot.platform.auth.security.AdminUserPrincipal;
import com.core.coreboot.platform.auth.service.AdminLoginAttemptRecorder;
import com.core.coreboot.platform.common.enums.RoleCode;
import com.core.coreboot.platform.common.enums.SysUserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.time.Instant;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminAuthenticationServiceImplTest {
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private AdminTokenService adminTokenService;
    @Mock
    private AdminLoginAttemptRecorder loginAttemptRecorder;

    private AdminAuthenticationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AdminAuthenticationServiceImpl(authenticationManager, adminTokenService, loginAttemptRecorder);
    }

    @Test
    void shouldReturnTokenAndRecordSuccessfulLogin() {
        AdminUserPrincipal principal = principal();
        UsernamePasswordAuthenticationToken authenticated = UsernamePasswordAuthenticationToken.authenticated(
                principal,
                null,
                principal.getAuthorities()
        );
        when(authenticationManager.authenticate(any())).thenReturn(authenticated);
        when(adminTokenService.issue(principal)).thenReturn(
                new AdminTokenService.IssuedAdminToken("token", Instant.parse("2030-01-01T00:00:00Z"), 3600)
        );

        AdminLoginResponse response = service.login(new AdminLoginRequest(" admin ", "Password!123"), "127.0.0.1");

        assertEquals("token", response.accessToken());
        assertEquals("Bearer", response.tokenType());
        assertEquals(10L, response.user().userId());
        verify(loginAttemptRecorder).recordSuccess(principal, "127.0.0.1");
    }

    @Test
    void shouldRecordFailedLoginWithoutRevealingAccountState() {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad credentials"));

        CustomException exception = assertThrows(
                CustomException.class,
                () -> service.login(new AdminLoginRequest("admin", "wrong"), "127.0.0.1")
        );

        assertEquals(ExceptionEnum.PLATFORM_ADMIN_LOGIN_FAILED.getCode(), exception.getCode());
        verify(loginAttemptRecorder).recordFailure("admin");
    }

    private AdminUserPrincipal principal() {
        return new AdminUserPrincipal(
                10L,
                "admin",
                "hash",
                "管理员",
                SysUserStatus.ACTIVE,
                null,
                0,
                Set.of(RoleCode.SUPER_ADMIN),
                Set.of()
        );
    }
}
