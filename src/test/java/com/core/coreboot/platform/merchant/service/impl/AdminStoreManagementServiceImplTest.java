package com.core.coreboot.platform.merchant.service.impl;

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
import com.core.coreboot.platform.merchant.model.AdminStoreCreateCommand;
import com.core.coreboot.platform.merchant.model.AdminStoreStatusChangeCommand;
import com.core.coreboot.platform.merchant.model.AdminStoreUpdateCommand;
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
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminStoreManagementServiceImplTest {
    @Mock
    private StoreMapper storeMapper;
    @Mock
    private MerchantMapper merchantMapper;
    @Mock
    private SysUserMapper sysUserMapper;
    @Mock
    private StaffAuthorityMapper staffAuthorityMapper;
    @Mock
    private AuditLogMapper auditLogMapper;

    private AdminStoreManagementServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AdminStoreManagementServiceImpl(
                storeMapper,
                merchantMapper,
                sysUserMapper,
                staffAuthorityMapper,
                auditLogMapper,
                new ObjectMapper().findAndRegisterModules(),
                Clock.fixed(Instant.parse("2026-09-09T08:00:00Z"), ZoneOffset.UTC)
        );
    }

    @Test
    void shouldCreateActiveStoreAndWriteAuditLog() {
        allowSuperAdmin();
        Merchant merchant = activeMerchant();
        when(merchantMapper.selectById(3L)).thenReturn(merchant);
        when(storeMapper.selectCount(any())).thenReturn(0L);
        when(storeMapper.insert(any(Store.class))).thenAnswer(invocation -> {
            Store store = invocation.getArgument(0);
            store.setId(10L);
            return 1;
        });
        when(auditLogMapper.insert(any(AuditLog.class))).thenReturn(1);

        var result = service.create(new AdminStoreCreateCommand(
                3L,
                "store-002",
                " 二号门店 ",
                " 测试路2号 ",
                " 13800138000 ",
                5L,
                "192.0.2.20"
        ));

        assertEquals(10L, result.storeId());
        assertEquals("STORE-002", result.storeCode());
        assertEquals("二号门店", result.storeName());
        assertEquals(StoreStatus.ACTIVE, result.status());

        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogMapper).insert(auditCaptor.capture());
        assertEquals("STORE_CREATED", auditCaptor.getValue().getAction());
        assertEquals("STORE-002", auditCaptor.getValue().getResourceNo());
    }

    @Test
    void shouldUpdateOnlyEditableStoreFields() {
        allowSuperAdmin();
        Store store = store(StoreStatus.ACTIVE);
        when(storeMapper.selectByIdForUpdate(10L)).thenReturn(store);
        when(storeMapper.updateById(store)).thenReturn(1);
        when(merchantMapper.selectById(3L)).thenReturn(activeMerchant());
        when(auditLogMapper.insert(any(AuditLog.class))).thenReturn(1);

        var result = service.update(new AdminStoreUpdateCommand(
                10L,
                "新门店名称",
                "新地址",
                "010-12345678",
                5L,
                null
        ));

        assertEquals("STORE-001", result.storeCode());
        assertEquals(3L, result.merchantId());
        assertEquals("新门店名称", result.storeName());
        assertEquals("新地址", result.address());
    }

    @Test
    void shouldRejectEnablingStoreWhenMerchantIsUnavailable() {
        allowSuperAdmin();
        Store store = store(StoreStatus.SUSPENDED);
        Merchant merchant = activeMerchant();
        merchant.setStatus(MerchantStatus.SUSPENDED);
        when(storeMapper.selectByIdForUpdate(10L)).thenReturn(store);
        when(merchantMapper.selectById(3L)).thenReturn(merchant);

        CustomException exception = assertThrows(CustomException.class, () ->
                service.changeStatus(new AdminStoreStatusChangeCommand(
                        10L,
                        StoreStatus.ACTIVE,
                        5L,
                        null
                ))
        );

        assertEquals(ExceptionEnum.PLATFORM_MERCHANT_UNAVAILABLE.getCode(), exception.getCode());
        verify(storeMapper, never()).updateById(any(Store.class));
        verify(auditLogMapper, never()).insert(any(AuditLog.class));
    }

    @Test
    void shouldDenyNonSuperAdmin() {
        when(sysUserMapper.selectById(5L)).thenReturn(SysUser.builder()
                .id(5L)
                .status(SysUserStatus.ACTIVE)
                .build());
        when(staffAuthorityMapper.selectRoleCodes(5L)).thenReturn(List.of(RoleCode.STORE_MANAGER.getCode()));

        CustomException exception = assertThrows(CustomException.class, () ->
                service.listActiveMerchantOptions(5L)
        );

        assertEquals(ExceptionEnum.PLATFORM_ADMIN_ACCESS_DENIED.getCode(), exception.getCode());
        verify(merchantMapper, never()).selectList(any());
    }

    private void allowSuperAdmin() {
        when(sysUserMapper.selectById(5L)).thenReturn(SysUser.builder()
                .id(5L)
                .status(SysUserStatus.ACTIVE)
                .build());
        when(staffAuthorityMapper.selectRoleCodes(5L)).thenReturn(List.of(RoleCode.SUPER_ADMIN.getCode()));
    }

    private Merchant activeMerchant() {
        return Merchant.builder()
                .id(3L)
                .merchantCode("MERCHANT-001")
                .businessName("测试商户")
                .status(MerchantStatus.ACTIVE)
                .build();
    }

    private Store store(StoreStatus status) {
        return Store.builder()
                .id(10L)
                .merchantId(3L)
                .storeCode("STORE-001")
                .storeName("一号门店")
                .address("测试路1号")
                .contactPhone("13800138000")
                .status(status)
                .build();
    }
}
