package com.core.coreboot.platform.auth.service.impl;

import com.core.coreboot.exception.CustomException;
import com.core.coreboot.exception.ExceptionEnum;
import com.core.coreboot.platform.audit.entity.AuditLog;
import com.core.coreboot.platform.audit.mapper.AuditLogMapper;
import com.core.coreboot.platform.common.enums.RoleCode;
import com.core.coreboot.platform.common.enums.SysUserStatus;
import com.core.coreboot.platform.staff.entity.SysUser;
import com.core.coreboot.platform.staff.mapper.SysUserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminPasswordServiceImplTest {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 10, 19, 0);
    private static final String CURRENT_PASSWORD = "CurrentPass!123";
    private static final String NEW_PASSWORD = "ChangedPass!456";

    @Mock
    private SysUserMapper sysUserMapper;
    @Mock
    private AuditLogMapper auditLogMapper;

    private PasswordEncoder passwordEncoder;
    private AdminPasswordServiceImpl service;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder(4);
        service = new AdminPasswordServiceImpl(
                sysUserMapper,
                passwordEncoder,
                auditLogMapper,
                Clock.fixed(Instant.parse("2026-09-10T11:00:00Z"), ZoneOffset.ofHours(8))
        );
    }

    @Test
    void shouldAllowClerkToChangeOwnPasswordAndInvalidateExistingTokens() {
        when(sysUserMapper.selectByIdForUpdate(8L)).thenReturn(activeUser());
        when(sysUserMapper.resetStaffPassword(any(), any(), any())).thenReturn(1);
        when(auditLogMapper.insert(any(AuditLog.class))).thenReturn(1);

        service.changeOwnPassword(
                8L,
                Set.of(RoleCode.CLERK),
                CURRENT_PASSWORD,
                NEW_PASSWORD,
                "192.0.2.80"
        );

        ArgumentCaptor<String> hashCaptor = ArgumentCaptor.forClass(String.class);
        verify(sysUserMapper).resetStaffPassword(
                org.mockito.ArgumentMatchers.eq(8L),
                hashCaptor.capture(),
                org.mockito.ArgumentMatchers.eq(NOW)
        );
        assertTrue(passwordEncoder.matches(NEW_PASSWORD, hashCaptor.getValue()));
        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogMapper).insert(auditCaptor.capture());
        assertEquals("ADMIN_PASSWORD_CHANGED", auditCaptor.getValue().getAction());
        assertEquals(RoleCode.CLERK, auditCaptor.getValue().getOperatorRole());
        assertEquals("192.0.2.80", auditCaptor.getValue().getClientIp());
        assertNull(auditCaptor.getValue().getBeforeSnapshot());
        assertNull(auditCaptor.getValue().getAfterSnapshot());
    }

    @Test
    void shouldRejectIncorrectCurrentPassword() {
        when(sysUserMapper.selectByIdForUpdate(8L)).thenReturn(activeUser());

        CustomException exception = assertThrows(
                CustomException.class,
                () -> service.changeOwnPassword(
                        8L,
                        Set.of(RoleCode.STORE_MANAGER),
                        "WrongPassword!1",
                        NEW_PASSWORD,
                        null
                )
        );

        assertEquals(ExceptionEnum.PLATFORM_ADMIN_CURRENT_PASSWORD_INCORRECT.getCode(), exception.getCode());
        verify(sysUserMapper, never()).resetStaffPassword(any(), any(), any());
        verify(auditLogMapper, never()).insert(any(AuditLog.class));
    }

    @Test
    void shouldRejectWeakNewPasswordBeforeLockingUser() {
        CustomException exception = assertThrows(
                CustomException.class,
                () -> service.changeOwnPassword(
                        8L,
                        Set.of(RoleCode.SUPER_ADMIN),
                        CURRENT_PASSWORD,
                        "weak-password",
                        null
                )
        );

        assertEquals(ExceptionEnum.PLATFORM_STAFF_PASSWORD_INVALID.getCode(), exception.getCode());
        verify(sysUserMapper, never()).selectByIdForUpdate(any());
    }

    private SysUser activeUser() {
        return SysUser.builder()
                .id(8L)
                .username("clerk")
                .passwordHash(passwordEncoder.encode(CURRENT_PASSWORD))
                .status(SysUserStatus.ACTIVE)
                .tokenVersion(2)
                .build();
    }
}
