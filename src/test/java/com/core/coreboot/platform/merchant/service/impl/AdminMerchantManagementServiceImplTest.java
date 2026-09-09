package com.core.coreboot.platform.merchant.service.impl;

import com.core.coreboot.exception.CustomException;
import com.core.coreboot.exception.ExceptionEnum;
import com.core.coreboot.platform.audit.entity.AuditLog;
import com.core.coreboot.platform.audit.mapper.AuditLogMapper;
import com.core.coreboot.platform.common.enums.MerchantStatus;
import com.core.coreboot.platform.common.enums.RoleCode;
import com.core.coreboot.platform.common.enums.SysUserStatus;
import com.core.coreboot.platform.merchant.entity.Merchant;
import com.core.coreboot.platform.merchant.mapper.MerchantMapper;
import com.core.coreboot.platform.merchant.model.AdminMerchantCreateCommand;
import com.core.coreboot.platform.merchant.model.AdminMerchantStatusChangeCommand;
import com.core.coreboot.platform.merchant.model.AdminMerchantUpdateCommand;
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
class AdminMerchantManagementServiceImplTest {
    @Mock
    private MerchantMapper merchantMapper;
    @Mock
    private SysUserMapper sysUserMapper;
    @Mock
    private StaffAuthorityMapper staffAuthorityMapper;
    @Mock
    private AuditLogMapper auditLogMapper;

    private AdminMerchantManagementServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AdminMerchantManagementServiceImpl(
                merchantMapper,
                sysUserMapper,
                staffAuthorityMapper,
                auditLogMapper,
                new ObjectMapper().findAndRegisterModules(),
                Clock.fixed(Instant.parse("2026-09-09T09:00:00Z"), ZoneOffset.UTC)
        );
    }

    @Test
    void shouldCreateActiveMerchantWithNormalizedCodes() {
        allowSuperAdmin();
        when(merchantMapper.selectCount(any())).thenReturn(0L);
        when(merchantMapper.insert(any(Merchant.class))).thenAnswer(invocation -> {
            Merchant merchant = invocation.getArgument(0);
            merchant.setId(8L);
            return 1;
        });
        when(auditLogMapper.insert(any(AuditLog.class))).thenReturn(1);

        var result = service.create(new AdminMerchantCreateCommand(
                "merchant-002",
                " 测试服务有限公司 ",
                " 测试商户 ",
                "91350100m000100y43",
                " 张三 ",
                " 13800138000 ",
                " AG-2026-002 ",
                5L,
                "192.0.2.20"
        ));

        assertEquals(8L, result.merchantId());
        assertEquals("MERCHANT-002", result.merchantCode());
        assertEquals("91350100M000100Y43", result.unifiedSocialCreditCode());
        assertEquals(MerchantStatus.ACTIVE, result.status());

        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogMapper).insert(auditCaptor.capture());
        assertEquals("MERCHANT_CREATED", auditCaptor.getValue().getAction());
        assertEquals("MERCHANT-002", auditCaptor.getValue().getResourceNo());
    }

    @Test
    void shouldRejectUsedCreditCode() {
        allowSuperAdmin();
        when(merchantMapper.selectCount(any())).thenReturn(0L, 1L);

        CustomException exception = assertThrows(CustomException.class, () ->
                service.create(new AdminMerchantCreateCommand(
                        "MERCHANT-002",
                        "测试服务有限公司",
                        "测试商户",
                        "91350100M000100Y43",
                        "张三",
                        "13800138000",
                        null,
                        5L,
                        null
                ))
        );

        assertEquals(ExceptionEnum.PLATFORM_MERCHANT_CREDIT_CODE_USED.getCode(), exception.getCode());
        verify(merchantMapper, never()).insert(any(Merchant.class));
    }

    @Test
    void shouldUpdateEditableFieldsWithoutChangingMerchantCode() {
        allowSuperAdmin();
        Merchant merchant = merchant(MerchantStatus.ACTIVE);
        when(merchantMapper.selectByIdForUpdate(8L)).thenReturn(merchant);
        when(merchantMapper.updateById(merchant)).thenReturn(1);
        when(auditLogMapper.insert(any(AuditLog.class))).thenReturn(1);

        var result = service.update(new AdminMerchantUpdateCommand(
                8L,
                "新签约主体",
                "新商户名称",
                null,
                "李四",
                "010-12345678",
                null,
                5L,
                null
        ));

        assertEquals("MERCHANT-001", result.merchantCode());
        assertEquals("新签约主体", result.legalName());
        assertEquals("新商户名称", result.businessName());
        assertEquals("李四", result.contactName());
    }

    @Test
    void shouldSuspendMerchantWithoutChangingStores() {
        allowSuperAdmin();
        Merchant merchant = merchant(MerchantStatus.ACTIVE);
        when(merchantMapper.selectByIdForUpdate(8L)).thenReturn(merchant);
        when(merchantMapper.updateById(merchant)).thenReturn(1);
        when(auditLogMapper.insert(any(AuditLog.class))).thenReturn(1);

        var result = service.changeStatus(new AdminMerchantStatusChangeCommand(
                8L,
                MerchantStatus.SUSPENDED,
                5L,
                null
        ));

        assertEquals(MerchantStatus.SUSPENDED, result.status());
        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogMapper).insert(auditCaptor.capture());
        assertEquals("MERCHANT_SUSPENDED", auditCaptor.getValue().getAction());
    }

    @Test
    void shouldRejectChangingTerminatedMerchantStatus() {
        allowSuperAdmin();
        when(merchantMapper.selectByIdForUpdate(8L)).thenReturn(merchant(MerchantStatus.TERMINATED));

        CustomException exception = assertThrows(CustomException.class, () ->
                service.changeStatus(new AdminMerchantStatusChangeCommand(
                        8L,
                        MerchantStatus.ACTIVE,
                        5L,
                        null
                ))
        );

        assertEquals(ExceptionEnum.PLATFORM_MERCHANT_STATUS_INVALID.getCode(), exception.getCode());
        verify(merchantMapper, never()).updateById(any(Merchant.class));
    }

    private void allowSuperAdmin() {
        when(sysUserMapper.selectById(5L)).thenReturn(SysUser.builder()
                .id(5L)
                .status(SysUserStatus.ACTIVE)
                .build());
        when(staffAuthorityMapper.selectRoleCodes(5L)).thenReturn(List.of(RoleCode.SUPER_ADMIN.getCode()));
    }

    private Merchant merchant(MerchantStatus status) {
        return Merchant.builder()
                .id(8L)
                .merchantCode("MERCHANT-001")
                .legalName("测试服务有限公司")
                .businessName("测试商户")
                .contactName("张三")
                .contactPhone("13800138000")
                .status(status)
                .build();
    }
}
