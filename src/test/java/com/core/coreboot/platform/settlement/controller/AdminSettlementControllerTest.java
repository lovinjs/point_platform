package com.core.coreboot.platform.settlement.controller;

import com.core.coreboot.platform.auth.security.AdminUserPrincipal;
import com.core.coreboot.platform.common.enums.RoleCode;
import com.core.coreboot.platform.common.enums.SettlementPeriodStatus;
import com.core.coreboot.platform.common.enums.SysUserStatus;
import com.core.coreboot.platform.settlement.model.SettlementGenerateCommand;
import com.core.coreboot.platform.settlement.model.SettlementGenerationResult;
import com.core.coreboot.platform.settlement.service.SettlementService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.prepost.PreAuthorize;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminSettlementControllerTest {
    @Mock
    private SettlementService settlementService;
    @InjectMocks
    private AdminSettlementController controller;

    @Test
    void shouldRestrictGenerationToSuperAdminAndDeriveOperatorIdentity() throws Exception {
        PreAuthorize classAuthorization = AdminSettlementController.class.getAnnotation(PreAuthorize.class);
        assertEquals(
                "hasAnyRole('SUPER_ADMIN', 'STORE_MANAGER')",
                classAuthorization.value()
        );
        PreAuthorize methodAuthorization = AdminSettlementController.class
                .getMethod(
                        "generate",
                        String.class,
                        AdminUserPrincipal.class,
                        jakarta.servlet.http.HttpServletRequest.class
                )
                .getAnnotation(PreAuthorize.class);
        assertEquals("hasRole('SUPER_ADMIN')", methodAuthorization.value());

        AdminUserPrincipal principal = new AdminUserPrincipal(
                5L,
                "platform.admin",
                "hash",
                "超级管理员",
                SysUserStatus.ACTIVE,
                null,
                0,
                Set.of(RoleCode.SUPER_ADMIN),
                Set.of()
        );
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.0.2.20");
        when(settlementService.generate(any())).thenReturn(new SettlementGenerationResult(
                "2026-08",
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 31),
                SettlementPeriodStatus.GENERATED,
                LocalDateTime.of(2026, 9, 8, 18, 0),
                0,
                List.of()
        ));

        controller.generate("2026-08", principal, request);

        ArgumentCaptor<SettlementGenerateCommand> commandCaptor =
                ArgumentCaptor.forClass(SettlementGenerateCommand.class);
        verify(settlementService).generate(commandCaptor.capture());
        assertEquals("2026-08", commandCaptor.getValue().periodCode());
        assertEquals(5L, commandCaptor.getValue().operatorId());
        assertEquals("192.0.2.20", commandCaptor.getValue().clientIp());
    }
}
