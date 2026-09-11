package com.core.coreboot.platform.setting.service.impl;

import com.core.coreboot.exception.CustomException;
import com.core.coreboot.exception.ExceptionEnum;
import com.core.coreboot.platform.audit.entity.AuditLog;
import com.core.coreboot.platform.audit.mapper.AuditLogMapper;
import com.core.coreboot.platform.common.enums.RoleCode;
import com.core.coreboot.platform.common.enums.SysUserStatus;
import com.core.coreboot.platform.setting.entity.PlatformBusinessSetting;
import com.core.coreboot.platform.setting.mapper.PlatformBusinessSettingMapper;
import com.core.coreboot.platform.setting.model.PlatformBusinessSettingUpdateCommand;
import com.core.coreboot.platform.staff.entity.SysUser;
import com.core.coreboot.platform.staff.mapper.StaffAuthorityMapper;
import com.core.coreboot.platform.staff.mapper.SysUserMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlatformBusinessSettingServiceImplTest {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 11, 9, 30);

    @Mock
    private PlatformBusinessSettingMapper settingMapper;
    @Mock
    private SysUserMapper sysUserMapper;
    @Mock
    private StaffAuthorityMapper staffAuthorityMapper;
    @Mock
    private AuditLogMapper auditLogMapper;

    private PlatformBusinessSettingServiceImpl service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-09-11T09:30:00Z"), ZoneOffset.UTC);
        service = new PlatformBusinessSettingServiceImpl(
                settingMapper,
                sysUserMapper,
                staffAuthorityMapper,
                auditLogMapper,
                new ObjectMapper(),
                clock
        );
    }

    @Test
    void shouldReturnFixedAndEditableBusinessRules() {
        allowSuperAdmin();
        when(settingMapper.selectById(1L)).thenReturn(setting());

        var result = service.getCurrent(5L);

        assertEquals(1, result.pointsPerYuan());
        assertFalse(result.bonusPointsEnabled());
        assertEquals(500, result.platformFeeRateBps());
        assertEquals(5, result.consumptionPendingTtlMinutes());
        assertEquals(2L, result.version());
        assertNull(result.lastUpdatedBy());
    }

    @Test
    void shouldUpdateSettingWithVersionAndAuditReason() {
        allowSuperAdmin();
        PlatformBusinessSetting setting = setting();
        when(settingMapper.selectByIdForUpdate(1L)).thenReturn(setting);
        when(settingMapper.updateWithVersion(1L, 600, 10, 2L, 5L, NOW)).thenReturn(1);
        when(auditLogMapper.insert(any(AuditLog.class))).thenReturn(1);

        var result = service.update(new PlatformBusinessSettingUpdateCommand(
                600,
                10,
                2L,
                "合作协议费率调整",
                5L,
                "192.0.2.10"
        ));

        assertEquals(600, result.platformFeeRateBps());
        assertEquals(10, result.consumptionPendingTtlMinutes());
        assertEquals(3L, result.version());
        assertEquals(5L, result.lastUpdatedBy());
        assertEquals("平台管理员", result.lastUpdatedByName());
        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogMapper).insert(auditCaptor.capture());
        assertEquals("PLATFORM_BUSINESS_SETTING_UPDATED", auditCaptor.getValue().getAction());
        assertEquals("合作协议费率调整", auditCaptor.getValue().getRemark());
    }

    @Test
    void shouldRejectStaleSettingVersion() {
        allowSuperAdmin();
        when(settingMapper.selectByIdForUpdate(1L)).thenReturn(setting());

        CustomException exception = assertThrows(CustomException.class, () ->
                service.update(new PlatformBusinessSettingUpdateCommand(
                        600, 10, 1L, "过期页面提交", 5L, null
                ))
        );

        assertEquals(ExceptionEnum.PLATFORM_DATA_CONFLICT.getCode(), exception.getCode());
        verify(settingMapper, never()).updateWithVersion(any(), any(Integer.class), any(Integer.class),
                any(Long.class), any(), any());
        verify(auditLogMapper, never()).insert(any(AuditLog.class));
    }

    @Test
    void shouldExposeCurrentPolicyWithoutAdminAuthorization() {
        when(settingMapper.selectById(1L)).thenReturn(setting());

        var policy = service.currentPolicy();

        assertEquals(500, policy.platformFeeRateBps());
        assertEquals(5, policy.consumptionPendingTtlMinutes());
        verify(sysUserMapper, never()).selectById(any());
    }

    @Test
    void shouldRejectMissingSetting() {
        CustomException exception = assertThrows(
                CustomException.class,
                () -> service.currentPolicy()
        );

        assertEquals(ExceptionEnum.PLATFORM_BUSINESS_SETTING_INVALID.getCode(), exception.getCode());
    }

    private void allowSuperAdmin() {
        when(sysUserMapper.selectById(5L)).thenReturn(SysUser.builder()
                .id(5L)
                .realName("平台管理员")
                .status(SysUserStatus.ACTIVE)
                .build());
        when(staffAuthorityMapper.selectRoleCodes(5L))
                .thenReturn(List.of(RoleCode.SUPER_ADMIN.getCode()));
    }

    private PlatformBusinessSetting setting() {
        return PlatformBusinessSetting.builder()
                .id(1L)
                .platformFeeRateBps(500)
                .consumptionPendingTtlMinutes(5)
                .version(2L)
                .createTime(NOW.minusDays(1))
                .updateTime(NOW.minusHours(1))
                .build();
    }
}
