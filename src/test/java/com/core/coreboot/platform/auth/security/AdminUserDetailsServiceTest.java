package com.core.coreboot.platform.auth.security;

import com.core.coreboot.platform.common.enums.RoleCode;
import com.core.coreboot.platform.common.enums.SysUserStatus;
import com.core.coreboot.platform.staff.entity.SysUser;
import com.core.coreboot.platform.staff.mapper.StaffAuthorityMapper;
import com.core.coreboot.platform.staff.mapper.SysUserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserDetailsServiceTest {
    @Mock
    private SysUserMapper sysUserMapper;
    @Mock
    private StaffAuthorityMapper staffAuthorityMapper;

    private AdminUserDetailsService service;

    @BeforeEach
    void setUp() {
        service = new AdminUserDetailsService(sysUserMapper, staffAuthorityMapper);
    }

    @Test
    void shouldLoadRolesAndStoreScope() {
        SysUser user = SysUser.builder()
                .id(8L)
                .username("manager")
                .passwordHash("hash")
                .realName("店长")
                .status(SysUserStatus.ACTIVE)
                .tokenVersion(2)
                .build();
        when(sysUserMapper.selectByUsername("manager")).thenReturn(user);
        when(staffAuthorityMapper.selectRoleCodes(8L)).thenReturn(List.of("STORE_MANAGER"));
        when(staffAuthorityMapper.selectStoreIds(8L)).thenReturn(List.of(11L, 12L));

        AdminUserPrincipal principal = (AdminUserPrincipal) service.loadUserByUsername(" manager ");

        assertEquals(8L, principal.getUserId());
        assertEquals(2, principal.getTokenVersion());
        assertEquals(Set.of(RoleCode.STORE_MANAGER), principal.getRoles());
        assertEquals(Set.of(11L, 12L), principal.getStoreIds());
        assertTrue(principal.isEnabled());
    }

    @Test
    void shouldRejectAccountWithoutActiveRole() {
        SysUser user = SysUser.builder()
                .id(8L)
                .username("orphan")
                .status(SysUserStatus.ACTIVE)
                .build();
        when(sysUserMapper.selectByUsername("orphan")).thenReturn(user);
        when(staffAuthorityMapper.selectRoleCodes(8L)).thenReturn(List.of());

        assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername("orphan"));
    }
}
