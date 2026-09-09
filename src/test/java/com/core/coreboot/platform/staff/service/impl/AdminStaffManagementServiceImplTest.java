package com.core.coreboot.platform.staff.service.impl;

import com.core.coreboot.exception.CustomException;
import com.core.coreboot.exception.ExceptionEnum;
import com.core.coreboot.platform.audit.entity.AuditLog;
import com.core.coreboot.platform.audit.mapper.AuditLogMapper;
import com.core.coreboot.platform.common.enums.MerchantStatus;
import com.core.coreboot.platform.common.enums.RoleCode;
import com.core.coreboot.platform.common.enums.StoreStatus;
import com.core.coreboot.platform.common.enums.SysUserStatus;
import com.core.coreboot.platform.merchant.entity.Merchant;
import com.core.coreboot.platform.merchant.entity.Store;
import com.core.coreboot.platform.merchant.mapper.MerchantMapper;
import com.core.coreboot.platform.merchant.mapper.StoreMapper;
import com.core.coreboot.platform.staff.entity.SysRole;
import com.core.coreboot.platform.staff.entity.SysUser;
import com.core.coreboot.platform.staff.entity.SysUserRole;
import com.core.coreboot.platform.staff.entity.SysUserStore;
import com.core.coreboot.platform.staff.mapper.StaffAuthorityMapper;
import com.core.coreboot.platform.staff.mapper.SysRoleMapper;
import com.core.coreboot.platform.staff.mapper.SysUserMapper;
import com.core.coreboot.platform.staff.mapper.SysUserRoleMapper;
import com.core.coreboot.platform.staff.mapper.SysUserStoreMapper;
import com.core.coreboot.platform.staff.model.AdminStaffCreateCommand;
import com.core.coreboot.platform.staff.model.AdminStaffPasswordResetCommand;
import com.core.coreboot.platform.staff.model.AdminStaffStatusChangeCommand;
import com.core.coreboot.platform.staff.model.AdminStaffUpdateCommand;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminStaffManagementServiceImplTest {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 10, 2, 0);

    @Mock
    private SysUserMapper sysUserMapper;
    @Mock
    private SysRoleMapper sysRoleMapper;
    @Mock
    private SysUserRoleMapper sysUserRoleMapper;
    @Mock
    private SysUserStoreMapper sysUserStoreMapper;
    @Mock
    private StaffAuthorityMapper staffAuthorityMapper;
    @Mock
    private StoreMapper storeMapper;
    @Mock
    private MerchantMapper merchantMapper;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuditLogMapper auditLogMapper;

    private AdminStaffManagementServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AdminStaffManagementServiceImpl(
                sysUserMapper,
                sysRoleMapper,
                sysUserRoleMapper,
                sysUserStoreMapper,
                staffAuthorityMapper,
                storeMapper,
                merchantMapper,
                passwordEncoder,
                auditLogMapper,
                new ObjectMapper().findAndRegisterModules(),
                Clock.fixed(Instant.parse("2026-09-10T02:00:00Z"), ZoneOffset.UTC)
        );
    }

    @Test
    void shouldCreateActiveClerkWithOneStoreAndHashedPassword() {
        allowSuperAdmin();
        when(sysRoleMapper.selectByRoleCode("CLERK")).thenReturn(role(11L, RoleCode.CLERK));
        allowAssignableStore(2L, "门店A");
        when(sysUserMapper.selectCount(any())).thenReturn(0L);
        when(passwordEncoder.encode("StrongPass@2026")).thenReturn("HASHED");
        when(sysUserMapper.insert(any(SysUser.class))).thenAnswer(invocation -> {
            SysUser user = invocation.getArgument(0);
            user.setId(8L);
            return 1;
        });
        when(sysUserRoleMapper.insert(any(SysUserRole.class))).thenReturn(1);
        when(sysUserStoreMapper.insert(any(SysUserStore.class))).thenReturn(1);
        when(auditLogMapper.insert(any(AuditLog.class))).thenReturn(1);

        var result = service.create(new AdminStaffCreateCommand(
                " Clerk.One ",
                "StrongPass@2026",
                " 测试店员 ",
                " 13900000003 ",
                RoleCode.CLERK,
                2L,
                1L,
                "192.0.2.10"
        ));

        assertEquals(8L, result.userId());
        assertEquals("clerk.one", result.username());
        assertEquals(SysUserStatus.ACTIVE, result.status());
        assertEquals(List.of(RoleCode.CLERK), result.roleCodes());
        assertEquals(2L, result.stores().getFirst().storeId());

        ArgumentCaptor<SysUser> userCaptor = ArgumentCaptor.forClass(SysUser.class);
        verify(sysUserMapper).insert(userCaptor.capture());
        assertEquals("HASHED", userCaptor.getValue().getPasswordHash());
        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogMapper).insert(auditCaptor.capture());
        assertEquals("STAFF_CREATED", auditCaptor.getValue().getAction());
    }

    @Test
    void shouldRejectCreatingSuperAdminThroughStaffManagement() {
        CustomException exception = assertThrows(CustomException.class, () ->
                service.create(new AdminStaffCreateCommand(
                        "another.admin",
                        "StrongPass@2026",
                        "管理员",
                        null,
                        RoleCode.SUPER_ADMIN,
                        2L,
                        1L,
                        null
                ))
        );

        assertEquals(ExceptionEnum.PLATFORM_STAFF_ROLE_INVALID.getCode(), exception.getCode());
        verify(sysUserMapper, never()).insert(any(SysUser.class));
    }

    @Test
    void shouldProtectSuperAdminFromEmployeeUpdates() {
        allowSuperAdmin();
        when(sysUserMapper.selectByIdForUpdate(9L)).thenReturn(staffUser(9L));
        when(sysUserRoleMapper.selectList(any())).thenReturn(List.of(SysUserRole.builder()
                .userId(9L)
                .roleId(13L)
                .build()));
        when(sysRoleMapper.selectByIds(any())).thenReturn(List.of(role(13L, RoleCode.SUPER_ADMIN)));

        CustomException exception = assertThrows(CustomException.class, () ->
                service.update(new AdminStaffUpdateCommand(
                        9L,
                        "不能修改",
                        null,
                        RoleCode.CLERK,
                        2L,
                        1L,
                        null
                ))
        );

        assertEquals(ExceptionEnum.PLATFORM_STAFF_PROTECTED_ACCOUNT.getCode(), exception.getCode());
        verify(sysUserMapper, never()).updateStaffProfile(any(), any(), any(), any(Integer.class), any());
    }

    @Test
    void shouldReplaceRoleAndStoreAndRevokeExistingToken() {
        allowSuperAdmin();
        SysUser user = staffUser(8L);
        allowManagedStaff(user, RoleCode.CLERK, 2L);
        when(sysRoleMapper.selectByRoleCode("STORE_MANAGER"))
                .thenReturn(role(12L, RoleCode.STORE_MANAGER));
        allowAssignableStore(3L, "门店B");
        when(storeMapper.selectByIds(any())).thenReturn(List.of(store(2L, "门店A")));
        when(merchantMapper.selectByIds(any())).thenReturn(List.of(merchant()));
        when(sysUserMapper.updateStaffProfile(8L, null, "新店长", 1, NOW)).thenReturn(1);
        when(sysUserRoleMapper.insert(any(SysUserRole.class))).thenReturn(1);
        when(sysUserStoreMapper.insert(any(SysUserStore.class))).thenReturn(1);
        when(auditLogMapper.insert(any(AuditLog.class))).thenReturn(1);

        var result = service.update(new AdminStaffUpdateCommand(
                8L,
                "新店长",
                null,
                RoleCode.STORE_MANAGER,
                3L,
                1L,
                null
        ));

        assertEquals(List.of(RoleCode.STORE_MANAGER), result.roleCodes());
        assertEquals(3L, result.stores().getFirst().storeId());
        assertEquals(3, user.getTokenVersion());
        verify(sysUserRoleMapper).delete(any());
        verify(sysUserStoreMapper).delete(any());
    }

    @Test
    void shouldDisableStaffAndClearTemporaryLock() {
        allowSuperAdmin();
        SysUser user = staffUser(8L);
        user.setFailedLoginCount(5);
        user.setLockedUntil(NOW.plusMinutes(10));
        allowManagedStaff(user, RoleCode.CLERK, 2L);
        when(storeMapper.selectByIds(any())).thenReturn(List.of(store(2L, "门店A")));
        when(merchantMapper.selectByIds(any())).thenReturn(List.of(merchant()));
        when(sysUserMapper.updateStaffStatus(8L, "DISABLED", NOW)).thenReturn(1);
        when(auditLogMapper.insert(any(AuditLog.class))).thenReturn(1);

        var result = service.changeStatus(new AdminStaffStatusChangeCommand(
                8L, SysUserStatus.DISABLED, 1L, null
        ));

        assertEquals(SysUserStatus.DISABLED, result.status());
        assertEquals(0, result.failedLoginCount());
        assertEquals(false, result.loginLocked());
        assertEquals(3, user.getTokenVersion());
    }

    @Test
    void shouldRejectResettingToCurrentPassword() {
        allowSuperAdmin();
        SysUser user = staffUser(8L);
        allowManagedStaff(user, RoleCode.CLERK, 2L);
        when(passwordEncoder.matches("StrongPass@2027", "OLD_HASH")).thenReturn(true);

        CustomException exception = assertThrows(CustomException.class, () ->
                service.resetPassword(new AdminStaffPasswordResetCommand(
                        8L, "StrongPass@2027", 1L, null
                ))
        );

        assertEquals(ExceptionEnum.PLATFORM_STAFF_PASSWORD_UNCHANGED.getCode(), exception.getCode());
        verify(sysUserMapper, never()).resetStaffPassword(any(), any(), any());
    }

    private void allowSuperAdmin() {
        when(sysUserMapper.selectById(1L)).thenReturn(SysUser.builder()
                .id(1L)
                .status(SysUserStatus.ACTIVE)
                .build());
        when(staffAuthorityMapper.selectRoleCodes(1L)).thenReturn(List.of(RoleCode.SUPER_ADMIN.getCode()));
    }

    private void allowAssignableStore(long storeId, String storeName) {
        when(storeMapper.selectById(storeId)).thenReturn(store(storeId, storeName));
        when(merchantMapper.selectById(7L)).thenReturn(merchant());
    }

    private void allowManagedStaff(SysUser user, RoleCode roleCode, long storeId) {
        when(sysUserMapper.selectByIdForUpdate(user.getId())).thenReturn(user);
        when(sysUserRoleMapper.selectList(any())).thenReturn(List.of(SysUserRole.builder()
                .userId(user.getId())
                .roleId(11L)
                .build()));
        when(sysRoleMapper.selectByIds(any())).thenReturn(List.of(role(11L, roleCode)));
        when(sysUserStoreMapper.selectList(any())).thenReturn(List.of(SysUserStore.builder()
                .userId(user.getId())
                .storeId(storeId)
                .build()));
    }

    private SysUser staffUser(long userId) {
        return SysUser.builder()
                .id(userId)
                .username("clerk.one")
                .passwordHash("OLD_HASH")
                .realName("测试店员")
                .status(SysUserStatus.ACTIVE)
                .failedLoginCount(0)
                .tokenVersion(2)
                .build();
    }

    private SysRole role(long roleId, RoleCode roleCode) {
        return SysRole.builder()
                .id(roleId)
                .roleCode(roleCode)
                .status("ACTIVE")
                .build();
    }

    private Store store(long storeId, String storeName) {
        return Store.builder()
                .id(storeId)
                .merchantId(7L)
                .storeCode("STORE-" + storeId)
                .storeName(storeName)
                .status(StoreStatus.ACTIVE)
                .build();
    }

    private Merchant merchant() {
        return Merchant.builder()
                .id(7L)
                .businessName("测试商户")
                .status(MerchantStatus.ACTIVE)
                .build();
    }
}
