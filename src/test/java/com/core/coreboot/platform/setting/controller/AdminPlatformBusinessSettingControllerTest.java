package com.core.coreboot.platform.setting.controller;

import com.core.coreboot.platform.auth.security.AdminUserPrincipal;
import com.core.coreboot.platform.common.enums.RoleCode;
import com.core.coreboot.platform.common.enums.SysUserStatus;
import com.core.coreboot.platform.setting.model.PlatformBusinessSettingUpdateCommand;
import com.core.coreboot.platform.setting.model.PlatformBusinessSettingUpdateRequest;
import com.core.coreboot.platform.setting.service.PlatformBusinessSettingService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminPlatformBusinessSettingControllerTest {
    @Mock
    private PlatformBusinessSettingService settingService;
    @Mock
    private HttpServletRequest servletRequest;
    @InjectMocks
    private AdminPlatformBusinessSettingController controller;

    @Test
    void shouldRestrictEndpointsToSuperAdmin() {
        PreAuthorize authorization = AdminPlatformBusinessSettingController.class
                .getAnnotation(PreAuthorize.class);

        assertEquals("hasRole('SUPER_ADMIN')", authorization.value());
    }

    @Test
    void shouldDeriveOperatorAndClientIpWhenUpdating() {
        when(servletRequest.getRemoteAddr()).thenReturn("192.0.2.20");
        PlatformBusinessSettingUpdateRequest request = new PlatformBusinessSettingUpdateRequest(
                650,
                8,
                3L,
                "调整平台合作费率"
        );

        controller.update(request, principal(), servletRequest);

        ArgumentCaptor<PlatformBusinessSettingUpdateCommand> commandCaptor =
                ArgumentCaptor.forClass(PlatformBusinessSettingUpdateCommand.class);
        verify(settingService).update(commandCaptor.capture());
        assertEquals(5L, commandCaptor.getValue().operatorId());
        assertEquals("192.0.2.20", commandCaptor.getValue().clientIp());
        assertEquals(650, commandCaptor.getValue().platformFeeRateBps());
        assertEquals(3L, commandCaptor.getValue().version());
    }

    private AdminUserPrincipal principal() {
        return new AdminUserPrincipal(
                5L,
                "admin",
                "hash",
                "平台管理员",
                SysUserStatus.ACTIVE,
                null,
                0,
                Set.of(RoleCode.SUPER_ADMIN),
                Set.of()
        );
    }
}
