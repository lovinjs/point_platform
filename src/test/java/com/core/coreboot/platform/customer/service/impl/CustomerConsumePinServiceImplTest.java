package com.core.coreboot.platform.customer.service.impl;

import com.core.coreboot.exception.CustomException;
import com.core.coreboot.exception.ExceptionEnum;
import com.core.coreboot.platform.audit.entity.AuditLog;
import com.core.coreboot.platform.audit.mapper.AuditLogMapper;
import com.core.coreboot.platform.common.enums.CustomerStatus;
import com.core.coreboot.platform.customer.config.CustomerPinProperties;
import com.core.coreboot.platform.customer.entity.CustomerSecurity;
import com.core.coreboot.platform.customer.entity.CustomerUser;
import com.core.coreboot.platform.customer.mapper.CustomerSecurityMapper;
import com.core.coreboot.platform.customer.mapper.CustomerUserMapper;
import com.core.coreboot.platform.customer.model.ConsumePinStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerConsumePinServiceImplTest {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 6, 10, 0);
    private static final String CURRENT_PIN = "258369";
    private static final String NEW_PIN = "369258";

    @Mock
    private CustomerUserMapper customerUserMapper;
    @Mock
    private CustomerSecurityMapper customerSecurityMapper;
    @Mock
    private AuditLogMapper auditLogMapper;

    private PasswordEncoder passwordEncoder;
    private CustomerConsumePinServiceImpl service;

    @BeforeEach
    void setUp() {
        CustomerPinProperties properties = new CustomerPinProperties();
        properties.setMaxFailedAttempts(5);
        properties.setLockDuration(Duration.ofMinutes(30));
        passwordEncoder = new BCryptPasswordEncoder(4);
        Clock clock = Clock.fixed(Instant.parse("2026-09-06T10:00:00Z"), ZoneOffset.UTC);
        service = new CustomerConsumePinServiceImpl(
                customerUserMapper,
                customerSecurityMapper,
                auditLogMapper,
                passwordEncoder,
                properties,
                clock
        );
    }

    @Test
    void shouldSetInitialPinAsHashAndWriteAudit() {
        allowActiveCustomer();
        when(customerSecurityMapper.insert(any(CustomerSecurity.class))).thenReturn(1);
        when(auditLogMapper.insert(any(AuditLog.class))).thenReturn(1);

        service.setInitialPin(1L, CURRENT_PIN, "192.0.2.31");

        ArgumentCaptor<CustomerSecurity> securityCaptor = ArgumentCaptor.forClass(CustomerSecurity.class);
        verify(customerSecurityMapper).insert(securityCaptor.capture());
        CustomerSecurity inserted = securityCaptor.getValue();
        assertNotEquals(CURRENT_PIN, inserted.getConsumePinHash());
        assertTrue(passwordEncoder.matches(CURRENT_PIN, inserted.getConsumePinHash()));
        assertEquals(0, inserted.getFailedCount());
        assertEquals(NOW, inserted.getPinUpdatedTime());

        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogMapper).insert(auditCaptor.capture());
        assertEquals("CUSTOMER_CONSUME_PIN_SET", auditCaptor.getValue().getAction());
        assertEquals("192.0.2.31", auditCaptor.getValue().getClientIp());
        assertNull(auditCaptor.getValue().getBeforeSnapshot());
        assertNull(auditCaptor.getValue().getAfterSnapshot());
    }

    @Test
    void shouldRejectWeakPinBeforeReadingCustomerData() {
        CustomException exception = assertThrows(
                CustomException.class,
                () -> service.setInitialPin(1L, "123456", "192.0.2.31")
        );

        assertEquals(ExceptionEnum.PLATFORM_CONSUME_PIN_FORMAT_INVALID.getCode(), exception.getCode());
        verify(customerUserMapper, never()).selectById(any());
    }

    @Test
    void shouldRejectSettingPinTwice() {
        allowActiveCustomer();
        when(customerSecurityMapper.selectByCustomerIdForUpdate(1L)).thenReturn(security(CURRENT_PIN, 0, null));

        CustomException exception = assertThrows(
                CustomException.class,
                () -> service.setInitialPin(1L, NEW_PIN, "192.0.2.31")
        );

        assertEquals(ExceptionEnum.PLATFORM_CONSUME_PIN_ALREADY_SET.getCode(), exception.getCode());
        verify(customerSecurityMapper, never()).insert(any(CustomerSecurity.class));
        verify(auditLogMapper, never()).insert(any(AuditLog.class));
    }

    @Test
    void shouldChangePinAndClearFailureState() {
        allowActiveCustomer();
        CustomerSecurity security = security(CURRENT_PIN, 2, NOW.minusMinutes(1));
        when(customerSecurityMapper.selectByCustomerIdForUpdate(1L)).thenReturn(security);
        when(customerSecurityMapper.updateById(any(CustomerSecurity.class))).thenReturn(1);
        when(auditLogMapper.insert(any(AuditLog.class))).thenReturn(1);

        service.changePin(1L, CURRENT_PIN, NEW_PIN, "192.0.2.32");

        assertTrue(passwordEncoder.matches(NEW_PIN, security.getConsumePinHash()));
        assertFalse(passwordEncoder.matches(CURRENT_PIN, security.getConsumePinHash()));
        assertEquals(0, security.getFailedCount());
        assertNull(security.getLockedUntil());
        assertEquals(NOW, security.getPinUpdatedTime());
        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogMapper).insert(auditCaptor.capture());
        assertEquals("CUSTOMER_CONSUME_PIN_CHANGED", auditCaptor.getValue().getAction());
    }

    @Test
    void shouldPersistIncorrectAttemptWithoutWritingAuditBeforeLock() {
        allowActiveCustomer();
        CustomerSecurity security = security(CURRENT_PIN, 1, null);
        when(customerSecurityMapper.selectByCustomerIdForUpdate(1L)).thenReturn(security);
        when(customerSecurityMapper.updateById(any(CustomerSecurity.class))).thenReturn(1);

        CustomException exception = assertThrows(
                CustomException.class,
                () -> service.verifyPin(1L, "135790", "192.0.2.33")
        );

        assertEquals(ExceptionEnum.PLATFORM_CONSUME_PIN_INCORRECT.getCode(), exception.getCode());
        assertEquals(2, security.getFailedCount());
        assertNull(security.getLockedUntil());
        verify(auditLogMapper, never()).insert(any(AuditLog.class));
    }

    @Test
    void shouldLockAndAuditAtMaximumAttempts() {
        allowActiveCustomer();
        CustomerSecurity security = security(CURRENT_PIN, 4, null);
        when(customerSecurityMapper.selectByCustomerIdForUpdate(1L)).thenReturn(security);
        when(customerSecurityMapper.updateById(any(CustomerSecurity.class))).thenReturn(1);
        when(auditLogMapper.insert(any(AuditLog.class))).thenReturn(1);

        CustomException exception = assertThrows(
                CustomException.class,
                () -> service.verifyPin(1L, "135790", "192.0.2.34")
        );

        assertEquals(ExceptionEnum.PLATFORM_CONSUME_PIN_LOCKED.getCode(), exception.getCode());
        assertEquals(5, security.getFailedCount());
        assertEquals(NOW.plusMinutes(30), security.getLockedUntil());
        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogMapper).insert(auditCaptor.capture());
        assertEquals("CUSTOMER_CONSUME_PIN_LOCKED", auditCaptor.getValue().getAction());
    }

    @Test
    void shouldRejectAttemptWhileLockIsActive() {
        allowActiveCustomer();
        CustomerSecurity security = security(CURRENT_PIN, 5, NOW.plusMinutes(10));
        when(customerSecurityMapper.selectByCustomerIdForUpdate(1L)).thenReturn(security);

        CustomException exception = assertThrows(
                CustomException.class,
                () -> service.verifyPin(1L, CURRENT_PIN, "192.0.2.35")
        );

        assertEquals(ExceptionEnum.PLATFORM_CONSUME_PIN_LOCKED.getCode(), exception.getCode());
        verify(customerSecurityMapper, never()).updateById(any(CustomerSecurity.class));
    }

    @Test
    void shouldVerifyAfterExpiredLockAndClearFailureState() {
        allowActiveCustomer();
        CustomerSecurity security = security(CURRENT_PIN, 5, NOW.minusSeconds(1));
        when(customerSecurityMapper.selectByCustomerIdForUpdate(1L)).thenReturn(security);
        when(customerSecurityMapper.updateById(any(CustomerSecurity.class))).thenReturn(1);

        service.verifyPin(1L, CURRENT_PIN, "192.0.2.36");

        assertEquals(0, security.getFailedCount());
        assertNull(security.getLockedUntil());
        verify(customerSecurityMapper).updateById(security);
    }

    @Test
    void shouldReturnUnconfiguredStatusWithoutExposingFailureCount() {
        allowActiveCustomer();

        ConsumePinStatus status = service.getStatus(1L);

        assertFalse(status.configured());
        assertFalse(status.locked());
        assertNull(status.lockedUntil());
        assertNull(status.pinUpdatedTime());
    }

    private void allowActiveCustomer() {
        when(customerUserMapper.selectById(1L))
                .thenReturn(CustomerUser.builder().id(1L).status(CustomerStatus.ACTIVE).build());
    }

    private CustomerSecurity security(String pin, int failedCount, LocalDateTime lockedUntil) {
        return CustomerSecurity.builder()
                .customerId(1L)
                .consumePinHash(passwordEncoder.encode(pin))
                .failedCount(failedCount)
                .lockedUntil(lockedUntil)
                .pinUpdatedTime(NOW.minusDays(1))
                .build();
    }
}
