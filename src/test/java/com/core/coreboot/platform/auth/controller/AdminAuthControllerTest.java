package com.core.coreboot.platform.auth.controller;

import com.core.coreboot.platform.auth.model.AdminChangePasswordRequest;
import com.core.coreboot.platform.auth.security.AdminUserPrincipal;
import com.core.coreboot.platform.auth.service.AdminAuthenticationService;
import com.core.coreboot.platform.auth.service.AdminPasswordService;
import com.core.coreboot.platform.common.enums.RoleCode;
import com.core.coreboot.platform.common.enums.SysUserStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AdminAuthControllerTest {
    @Mock
    private AdminAuthenticationService authenticationService;
    @Mock
    private AdminPasswordService passwordService;
    @InjectMocks
    private AdminAuthController controller;

    @Test
    void shouldChangePasswordForAuthenticatedRoleUsingPrincipalIdentity() {
        AdminUserPrincipal principal = new AdminUserPrincipal(
                18L,
                "manager",
                "hash",
                "门店店长",
                SysUserStatus.ACTIVE,
                null,
                0,
                Set.of(RoleCode.STORE_MANAGER),
                Set.of(3L)
        );
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.setRemoteAddr("192.0.2.81");

        var response = controller.changePassword(
                new AdminChangePasswordRequest("CurrentPass!123", "ChangedPass!456"),
                principal,
                servletRequest
        );

        assertEquals(200, response.getCode());
        verify(passwordService).changeOwnPassword(
                18L,
                Set.of(RoleCode.STORE_MANAGER),
                "CurrentPass!123",
                "ChangedPass!456",
                "192.0.2.81"
        );
    }
}
