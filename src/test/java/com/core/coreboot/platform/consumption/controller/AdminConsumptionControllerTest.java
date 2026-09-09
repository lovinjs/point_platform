package com.core.coreboot.platform.consumption.controller;

import com.core.coreboot.platform.auth.security.AdminUserPrincipal;
import com.core.coreboot.platform.common.enums.ConsumptionOrderStatus;
import com.core.coreboot.platform.common.enums.ConsumptionVerificationMode;
import com.core.coreboot.platform.common.enums.RoleCode;
import com.core.coreboot.platform.common.enums.SysUserStatus;
import com.core.coreboot.platform.consumption.model.AdminConsumptionOrderStatusView;
import com.core.coreboot.platform.consumption.model.AdminPrepareConsumptionRequest;
import com.core.coreboot.platform.consumption.model.PrepareConsumptionCommand;
import com.core.coreboot.platform.consumption.model.PrepareConsumptionResult;
import com.core.coreboot.platform.consumption.service.AdminConsumptionOrderQueryService;
import com.core.coreboot.platform.consumption.service.PrepareConsumptionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminConsumptionControllerTest {
    @Mock
    private PrepareConsumptionService prepareConsumptionService;
    @Mock
    private AdminConsumptionOrderQueryService orderQueryService;
    @InjectMocks
    private AdminConsumptionController controller;

    @Test
    void shouldDeriveOperatorAndClientIpFromAuthentication() {
        AdminUserPrincipal principal = new AdminUserPrincipal(
                7L,
                "demo.clerk",
                "hash",
                "演示店员",
                SysUserStatus.ACTIVE,
                null,
                0,
                Set.of(RoleCode.CLERK),
                Set.of(20L)
        );
        AdminPrepareConsumptionRequest request = new AdminPrepareConsumptionRequest(
                10L, 20L, 30L, "现场消费"
        );
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.setRemoteAddr("192.0.2.30");
        when(prepareConsumptionService.prepare(any())).thenReturn(new PrepareConsumptionResult(
                "CSM-100", 10L, 20L, 30L, 3_000L, 500, 150L, 2_850L,
                ConsumptionVerificationMode.CUSTOMER_PIN,
                LocalDateTime.now().plusMinutes(5),
                ConsumptionOrderStatus.PENDING_CONFIRM
        ));

        controller.prepare(request, principal, "prepare-100", servletRequest);

        ArgumentCaptor<PrepareConsumptionCommand> commandCaptor =
                ArgumentCaptor.forClass(PrepareConsumptionCommand.class);
        verify(prepareConsumptionService).prepare(commandCaptor.capture());
        PrepareConsumptionCommand command = commandCaptor.getValue();
        assertEquals(7L, command.operatorId());
        assertEquals("prepare-100", command.idempotencyKey());
        assertEquals("192.0.2.30", command.clientIp());
    }

    @Test
    void shouldQueryOrderStatusForAuthenticatedOperator() {
        AdminUserPrincipal principal = new AdminUserPrincipal(
                7L,
                "demo.clerk",
                "hash",
                "演示店员",
                SysUserStatus.ACTIVE,
                null,
                0,
                Set.of(RoleCode.CLERK),
                Set.of(20L)
        );
        when(orderQueryService.getStatus(7L, "CSM-100"))
                .thenReturn(new AdminConsumptionOrderStatusView(
                        "CSM-100",
                        10L,
                        20L,
                        30L,
                        3_000L,
                        ConsumptionVerificationMode.CUSTOMER_PIN,
                        ConsumptionOrderStatus.PENDING_CONFIRM,
                        "现场消费",
                        LocalDateTime.now().plusMinutes(5),
                        null,
                        null,
                        LocalDateTime.now()
                ));

        var response = controller.status("CSM-100", principal);

        assertEquals("CSM-100", response.getData().orderNo());
        verify(orderQueryService).getStatus(7L, "CSM-100");
    }
}
