package com.core.coreboot.platform.customer.service.impl;

import com.core.coreboot.exception.CustomException;
import com.core.coreboot.exception.ExceptionEnum;
import com.core.coreboot.platform.customer.config.CustomerPinProperties;
import com.core.coreboot.platform.customer.model.ConsumePinStatus;
import com.core.coreboot.platform.customer.phone.model.PhoneVerificationDispatchResult;
import com.core.coreboot.platform.customer.phone.service.CustomerPhoneService;
import com.core.coreboot.platform.customer.service.ConsumePinResetTokenStore;
import com.core.coreboot.platform.customer.service.CustomerConsumePinService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerConsumePinRecoveryServiceImplTest {
    @Mock
    private CustomerPhoneService customerPhoneService;
    @Mock
    private ConsumePinResetTokenStore resetTokenStore;
    @Mock
    private CustomerConsumePinService consumePinService;

    private CustomerConsumePinRecoveryServiceImpl service;

    @BeforeEach
    void setUp() {
        CustomerPinProperties properties = new CustomerPinProperties();
        properties.setResetTokenTtl(Duration.ofMinutes(5));
        service = new CustomerConsumePinRecoveryServiceImpl(
                customerPhoneService,
                resetTokenStore,
                consumePinService,
                properties
        );
    }

    @Test
    void shouldSendResetCodeOnlyWhenPinIsConfigured() {
        PhoneVerificationDispatchResult dispatch = new PhoneVerificationDispatchResult(300, 60);
        when(consumePinService.getStatus(6L)).thenReturn(configuredStatus());
        when(customerPhoneService.requestBoundPhoneVerificationCode(6L)).thenReturn(dispatch);

        assertSame(dispatch, service.requestVerificationCode(6L));
    }

    @Test
    void shouldVerifyCodeAndIssueShortLivedResetToken() {
        when(consumePinService.getStatus(6L)).thenReturn(configuredStatus());
        when(resetTokenStore.issue(6L, Duration.ofMinutes(5))).thenReturn("reset-token");

        var result = service.verifyAndIssueResetToken(6L, "825391");

        assertEquals("reset-token", result.resetToken());
        assertEquals(300L, result.expiresInSeconds());
        verify(customerPhoneService).verifyBoundPhoneVerificationCode(6L, "825391");
    }

    @Test
    void shouldRejectRecoveryWhenPinHasNotBeenConfigured() {
        when(consumePinService.getStatus(6L)).thenReturn(new ConsumePinStatus(false, false, null, null));

        CustomException exception = assertThrows(
                CustomException.class,
                () -> service.requestVerificationCode(6L)
        );

        assertEquals(ExceptionEnum.PLATFORM_CONSUME_PIN_NOT_SET.getCode(), exception.getCode());
        verify(customerPhoneService, never()).requestBoundPhoneVerificationCode(6L);
    }

    @Test
    void shouldDelegateResetWithoutConsumingTokenOutsideDatabaseOperation() {
        service.resetPin(6L, "reset-token", "258369", "192.0.2.82");

        verify(consumePinService).resetPin(6L, "reset-token", "258369", "192.0.2.82");
        verify(resetTokenStore, never()).consume(6L, "reset-token");
    }

    private ConsumePinStatus configuredStatus() {
        return new ConsumePinStatus(true, false, null, null);
    }
}
