package com.core.coreboot.platform.bootstrap.service;

import com.core.coreboot.platform.audit.entity.AuditLog;
import com.core.coreboot.platform.audit.mapper.AuditLogMapper;
import com.core.coreboot.platform.bootstrap.config.AdminBootstrapProperties;
import com.core.coreboot.platform.common.enums.RoleCode;
import com.core.coreboot.platform.staff.entity.SysRole;
import com.core.coreboot.platform.staff.entity.SysUser;
import com.core.coreboot.platform.staff.entity.SysUserRole;
import com.core.coreboot.platform.staff.mapper.SysRoleMapper;
import com.core.coreboot.platform.staff.mapper.SysUserMapper;
import com.core.coreboot.platform.staff.mapper.SysUserRoleMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InitialSuperAdminBootstrapTest {
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private SysUserMapper sysUserMapper;
    @Mock
    private SysRoleMapper sysRoleMapper;
    @Mock
    private SysUserRoleMapper sysUserRoleMapper;
    @Mock
    private AuditLogMapper auditLogMapper;
    @Mock
    private ApplicationArguments applicationArguments;

    private AdminBootstrapProperties properties;
    private InitialSuperAdminBootstrap bootstrap;

    @BeforeEach
    void setUp() {
        properties = new AdminBootstrapProperties();
        bootstrap = new InitialSuperAdminBootstrap(
                properties,
                passwordEncoder,
                sysUserMapper,
                sysRoleMapper,
                sysUserRoleMapper,
                auditLogMapper
        );
    }

    @Test
    void shouldDoNothingWhenBootstrapIsDisabled() {
        bootstrap.run(applicationArguments);

        verify(sysRoleMapper, never()).countAssignedSuperAdmins();
        verify(sysUserMapper, never()).insert(any(SysUser.class));
    }

    @Test
    void shouldCreateFirstSuperAdminWithEncodedPassword() {
        properties.setEnabled(true);
        properties.setUsername("platform.admin");
        properties.setPassword("StrongPassword!123");
        properties.setRealName("平台管理员");
        properties.setPhone("13800000000");

        when(sysRoleMapper.countAssignedSuperAdmins()).thenReturn(0L);
        when(sysUserMapper.selectByUsername("platform.admin")).thenReturn(null);
        when(sysRoleMapper.selectByRoleCode("SUPER_ADMIN")).thenReturn(
                SysRole.builder().id(5L).roleCode(RoleCode.SUPER_ADMIN).status("ACTIVE").build()
        );
        when(passwordEncoder.encode("StrongPassword!123")).thenReturn("bcrypt-hash");
        when(sysUserMapper.insert(any(SysUser.class))).thenAnswer(invocation -> {
            SysUser user = invocation.getArgument(0);
            user.setId(9L);
            return 1;
        });
        when(sysUserRoleMapper.insert(any(SysUserRole.class))).thenReturn(1);
        when(auditLogMapper.insert(any(AuditLog.class))).thenReturn(1);

        bootstrap.run(applicationArguments);

        ArgumentCaptor<SysUser> userCaptor = ArgumentCaptor.forClass(SysUser.class);
        verify(sysUserMapper).insert(userCaptor.capture());
        SysUser savedUser = userCaptor.getValue();
        assertEquals("bcrypt-hash", savedUser.getPasswordHash());
        assertNotEquals("StrongPassword!123", savedUser.getPasswordHash());
        assertEquals(0, savedUser.getTokenVersion());
        assertNotNull(savedUser.getPasswordUpdatedTime());

        ArgumentCaptor<SysUserRole> relationCaptor = ArgumentCaptor.forClass(SysUserRole.class);
        verify(sysUserRoleMapper).insert(relationCaptor.capture());
        assertEquals(9L, relationCaptor.getValue().getUserId());
        assertEquals(5L, relationCaptor.getValue().getRoleId());
    }
}
