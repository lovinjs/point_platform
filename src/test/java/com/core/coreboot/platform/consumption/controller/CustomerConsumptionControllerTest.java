package com.core.coreboot.platform.consumption.controller;

import com.core.coreboot.platform.common.enums.ConsumptionOrderStatus;
import com.core.coreboot.platform.common.enums.CustomerStatus;
import com.core.coreboot.platform.consumption.model.ConfirmConsumptionRequest;
import com.core.coreboot.platform.consumption.model.ConsumptionConfirmationResult;
import com.core.coreboot.platform.consumption.service.CustomerConsumptionService;
import com.core.coreboot.platform.customer.auth.security.CustomerPrincipal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerConsumptionControllerTest {
    @Mock
    private CustomerConsumptionService customerConsumptionService;
    @InjectMocks
    private CustomerConsumptionController controller;

    @Test
    void shouldDeriveCustomerAndClientIpFromAuthentication() {
        CustomerPrincipal principal = new CustomerPrincipal(7L, 0, CustomerStatus.ACTIVE);
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.setRemoteAddr("192.0.2.52");
        ConsumptionConfirmationResult result = new ConsumptionConfirmationResult(
                "CSM123",
                2L,
                "测试门店",
                30L,
                3_000L,
                70L,
                ConsumptionOrderStatus.COMPLETED,
                LocalDateTime.of(2026, 9, 8, 12, 0)
        );
        when(customerConsumptionService.confirm(7L, "CSM123", "258369", "192.0.2.52"))
                .thenReturn(result);

        var response = controller.confirm(
                "CSM123",
                new ConfirmConsumptionRequest("258369"),
                principal,
                servletRequest
        );

        verify(customerConsumptionService).confirm(7L, "CSM123", "258369", "192.0.2.52");
        assertEquals(result, response.getData());
    }
}
