package com.core.coreboot.platform.customer.controller;

import com.core.coreboot.platform.common.enums.CustomerStatus;
import com.core.coreboot.platform.customer.auth.security.CustomerPrincipal;
import com.core.coreboot.platform.customer.model.ConsumePinStatus;
import com.core.coreboot.platform.customer.model.SetConsumePinRequest;
import com.core.coreboot.platform.customer.service.CustomerConsumePinService;
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
class CustomerConsumePinControllerTest {
    @Mock
    private CustomerConsumePinService customerConsumePinService;
    @InjectMocks
    private CustomerConsumePinController controller;

    @Test
    void shouldDeriveCustomerAndClientIpFromAuthentication() {
        CustomerPrincipal principal = new CustomerPrincipal(7L, 0, CustomerStatus.ACTIVE);
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.setRemoteAddr("192.0.2.40");
        ConsumePinStatus status = new ConsumePinStatus(
                true,
                false,
                null,
                LocalDateTime.of(2026, 9, 8, 10, 0)
        );
        when(customerConsumePinService.getStatus(7L)).thenReturn(status);

        var response = controller.setInitialPin(
                new SetConsumePinRequest("258369"),
                principal,
                servletRequest
        );

        verify(customerConsumePinService).setInitialPin(7L, "258369", "192.0.2.40");
        assertEquals(status, response.getData());
    }
}
