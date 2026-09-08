package com.core.coreboot.platform.recharge.controller;

import com.core.coreboot.platform.auth.security.AdminUserPrincipal;
import com.core.coreboot.platform.common.enums.RechargeRefundStatus;
import com.core.coreboot.platform.common.enums.RefundMethod;
import com.core.coreboot.platform.common.enums.RoleCode;
import com.core.coreboot.platform.common.enums.SysUserStatus;
import com.core.coreboot.platform.recharge.model.AdminRechargeRefundRequest;
import com.core.coreboot.platform.recharge.model.RechargeRefundCommand;
import com.core.coreboot.platform.recharge.model.RechargeRefundResult;
import com.core.coreboot.platform.recharge.service.RechargeRefundService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.prepost.PreAuthorize;

import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminRechargeRefundControllerTest {
    @Mock
    private RechargeRefundService rechargeRefundService;
    @InjectMocks
    private AdminRechargeRefundController controller;

    @Test
    void shouldRestrictControllerAndDeriveOperatorFromAuthentication() {
        PreAuthorize authorization = AdminRechargeRefundController.class.getAnnotation(PreAuthorize.class);
        assertEquals("hasRole('SUPER_ADMIN')", authorization.value());

        AdminUserPrincipal principal = new AdminUserPrincipal(
                3L,
                "platform.admin",
                "hash",
                "超级管理员",
                SysUserStatus.ACTIVE,
                null,
                0,
                Set.of(RoleCode.SUPER_ADMIN),
                Set.of()
        );
        AdminRechargeRefundRequest request = new AdminRechargeRefundRequest(
                RefundMethod.OTHER,
                "LOCAL-REFUND-001",
                "客户申请退款"
        );
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.setRemoteAddr("192.0.2.60");
        when(rechargeRefundService.refund(any())).thenReturn(new RechargeRefundResult(
                "RFD100",
                "RCH100",
                7L,
                1_000L,
                100_000L,
                RefundMethod.OTHER,
                RechargeRefundStatus.COMPLETED,
                0L,
                LocalDateTime.of(2026, 9, 8, 18, 0)
        ));

        controller.refund("RCH100", request, principal, "refund-request-100", servletRequest);

        ArgumentCaptor<RechargeRefundCommand> commandCaptor =
                ArgumentCaptor.forClass(RechargeRefundCommand.class);
        verify(rechargeRefundService).refund(commandCaptor.capture());
        assertEquals("RCH100", commandCaptor.getValue().orderNo());
        assertEquals(3L, commandCaptor.getValue().operatorId());
        assertEquals("refund-request-100", commandCaptor.getValue().idempotencyKey());
        assertEquals("192.0.2.60", commandCaptor.getValue().clientIp());
    }
}
