package com.core.coreboot.platform.recharge.controller;

import com.core.coreboot.platform.auth.security.AdminUserPrincipal;
import com.core.coreboot.platform.common.enums.PaymentMethod;
import com.core.coreboot.platform.common.enums.RechargeOrderStatus;
import com.core.coreboot.platform.common.enums.RoleCode;
import com.core.coreboot.platform.common.enums.SysUserStatus;
import com.core.coreboot.platform.recharge.model.AdminOfflineRechargeRequest;
import com.core.coreboot.platform.recharge.model.OfflineRechargeCommand;
import com.core.coreboot.platform.recharge.model.OfflineRechargeResult;
import com.core.coreboot.platform.recharge.service.OfflineRechargeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminRechargeControllerTest {
    @Mock
    private OfflineRechargeService offlineRechargeService;
    @InjectMocks
    private AdminRechargeController controller;

    @Test
    void shouldDeriveOperatorAndClientIpFromAuthenticatedRequest() {
        AdminUserPrincipal principal = new AdminUserPrincipal(
                7L,
                "admin",
                "hash",
                "管理员",
                SysUserStatus.ACTIVE,
                null,
                0,
                Set.of(RoleCode.SUPER_ADMIN),
                Set.of()
        );
        AdminOfflineRechargeRequest request = new AdminOfflineRechargeRequest(
                10L,
                20L,
                100L,
                PaymentMethod.BANK_TRANSFER,
                "PLATFORM-TX-100",
                "柜台核对"
        );
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.setRemoteAddr("192.0.2.10");
        when(offlineRechargeService.recharge(any())).thenReturn(new OfflineRechargeResult(
                "RCH-100",
                10L,
                20L,
                10_000L,
                100L,
                RechargeOrderStatus.COMPLETED
        ));

        controller.offlineRecharge(request, principal, "request-100", servletRequest);

        ArgumentCaptor<OfflineRechargeCommand> commandCaptor =
                ArgumentCaptor.forClass(OfflineRechargeCommand.class);
        verify(offlineRechargeService).recharge(commandCaptor.capture());
        OfflineRechargeCommand command = commandCaptor.getValue();
        assertEquals(7L, command.operatorId());
        assertEquals(10_000L, command.amountCent());
        assertEquals("request-100", command.idempotencyKey());
        assertEquals("192.0.2.10", command.clientIp());
    }
}
