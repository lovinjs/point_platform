package com.core.coreboot.platform.customer.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.core.coreboot.exception.CustomException;
import com.core.coreboot.exception.ExceptionEnum;
import com.core.coreboot.platform.audit.entity.AuditLog;
import com.core.coreboot.platform.audit.mapper.AuditLogMapper;
import com.core.coreboot.platform.common.enums.CustomerStatus;
import com.core.coreboot.platform.common.enums.RoleCode;
import com.core.coreboot.platform.common.enums.SysUserStatus;
import com.core.coreboot.platform.consumption.mapper.ConsumptionOrderMapper;
import com.core.coreboot.platform.customer.entity.CustomerSecurity;
import com.core.coreboot.platform.customer.entity.CustomerUser;
import com.core.coreboot.platform.customer.mapper.CustomerSecurityMapper;
import com.core.coreboot.platform.customer.mapper.CustomerUserMapper;
import com.core.coreboot.platform.customer.model.AdminCustomerStatusChangeCommand;
import com.core.coreboot.platform.customer.service.CustomerTransactionQueryService;
import com.core.coreboot.platform.point.entity.PointAccount;
import com.core.coreboot.platform.point.mapper.PointAccountMapper;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminCustomerManagementServiceImplTest {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 10, 8, 0);

    @Mock
    private CustomerUserMapper customerUserMapper;
    @Mock
    private CustomerSecurityMapper customerSecurityMapper;
    @Mock
    private PointAccountMapper pointAccountMapper;
    @Mock
    private ConsumptionOrderMapper consumptionOrderMapper;
    @Mock
    private CustomerTransactionQueryService transactionQueryService;
    @Mock
    private SysUserMapper sysUserMapper;
    @Mock
    private StaffAuthorityMapper staffAuthorityMapper;
    @Mock
    private AuditLogMapper auditLogMapper;

    private AdminCustomerManagementServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AdminCustomerManagementServiceImpl(
                customerUserMapper,
                customerSecurityMapper,
                pointAccountMapper,
                consumptionOrderMapper,
                transactionQueryService,
                sysUserMapper,
                staffAuthorityMapper,
                auditLogMapper,
                new ObjectMapper().findAndRegisterModules(),
                Clock.fixed(Instant.parse("2026-09-10T08:00:00Z"), ZoneOffset.UTC)
        );
    }

    @Test
    void shouldListCustomersWithBalanceAndConsumePinState() {
        allowSuperAdmin();
        CustomerUser customer = customer(CustomerStatus.ACTIVE);
        Page<CustomerUser> page = new Page<CustomerUser>(1, 20)
                .setRecords(List.of(customer))
                .setTotal(1);
        when(customerUserMapper.selectPage(any(Page.class), any())).thenReturn(page);
        when(pointAccountMapper.selectList(any())).thenReturn(List.of(
                PointAccount.builder().customerId(10L).availablePoints(128L).build()
        ));
        when(customerSecurityMapper.selectList(any())).thenReturn(List.of(
                CustomerSecurity.builder()
                        .customerId(10L)
                        .consumePinHash("hash")
                        .lockedUntil(NOW.plusMinutes(10))
                        .build()
        ));

        var result = service.list(5L, 1, 20, null, " 测试 ");

        assertEquals(1L, result.total());
        assertEquals(128L, result.items().getFirst().availablePoints());
        assertTrue(result.items().getFirst().consumePinConfigured());
        assertTrue(result.items().getFirst().consumePinLocked());
    }

    @Test
    void shouldFreezeCustomerInvalidateTokenCancelPendingOrderAndAuditReason() {
        allowSuperAdmin();
        CustomerUser customer = customer(CustomerStatus.ACTIVE);
        when(customerUserMapper.selectByIdForUpdate(10L)).thenReturn(customer);
        when(customerUserMapper.updateCustomerStatus(10L, "DISABLED", NOW)).thenReturn(1);
        when(consumptionOrderMapper.cancelPendingByCustomerId(10L, NOW)).thenReturn(1);
        when(auditLogMapper.insert(any(AuditLog.class))).thenReturn(1);

        var result = service.changeStatus(new AdminCustomerStatusChangeCommand(
                10L,
                CustomerStatus.DISABLED,
                " 疑似账户被盗，客服工单 CS-001 ",
                5L,
                "192.0.2.30"
        ));

        assertEquals(CustomerStatus.DISABLED, result.status());
        assertEquals(4, customer.getTokenVersion());
        verify(consumptionOrderMapper).cancelPendingByCustomerId(10L, NOW);
        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogMapper).insert(auditCaptor.capture());
        assertEquals("CUSTOMER_DISABLED", auditCaptor.getValue().getAction());
        assertEquals("疑似账户被盗，客服工单 CS-001", auditCaptor.getValue().getRemark());
        assertTrue(auditCaptor.getValue().getAfterSnapshot()
                .contains("\"cancelledPendingConsumptionOrders\":1"));
    }

    @Test
    void shouldRestoreCustomerWithoutRestoringCancelledOrders() {
        allowSuperAdmin();
        CustomerUser customer = customer(CustomerStatus.DISABLED);
        when(customerUserMapper.selectByIdForUpdate(10L)).thenReturn(customer);
        when(customerUserMapper.updateCustomerStatus(10L, "ACTIVE", NOW)).thenReturn(1);
        when(auditLogMapper.insert(any(AuditLog.class))).thenReturn(1);

        var result = service.changeStatus(new AdminCustomerStatusChangeCommand(
                10L,
                CustomerStatus.ACTIVE,
                "身份核验完成",
                5L,
                null
        ));

        assertEquals(CustomerStatus.ACTIVE, result.status());
        verify(consumptionOrderMapper, never()).cancelPendingByCustomerId(any(), any());
        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogMapper).insert(auditCaptor.capture());
        assertEquals("CUSTOMER_ENABLED", auditCaptor.getValue().getAction());
        assertTrue(auditCaptor.getValue().getAfterSnapshot()
                .contains("\"cancelledPendingConsumptionOrders\":0"));
    }

    @Test
    void shouldRejectBlankStatusReason() {
        CustomException exception = assertThrows(CustomException.class, () ->
                service.changeStatus(new AdminCustomerStatusChangeCommand(
                        10L,
                        CustomerStatus.DISABLED,
                        "  ",
                        5L,
                        null
                ))
        );

        assertEquals(ExceptionEnum.PLATFORM_CUSTOMER_STATUS_INVALID.getCode(), exception.getCode());
        verify(sysUserMapper, never()).selectById(any());
    }

    @Test
    void shouldDenyNonSuperAdmin() {
        when(sysUserMapper.selectById(5L)).thenReturn(SysUser.builder()
                .id(5L)
                .status(SysUserStatus.ACTIVE)
                .build());
        when(staffAuthorityMapper.selectRoleCodes(5L))
                .thenReturn(List.of(RoleCode.STORE_MANAGER.getCode()));

        CustomException exception = assertThrows(CustomException.class, () ->
                service.get(5L, 10L)
        );

        assertEquals(ExceptionEnum.PLATFORM_ADMIN_ACCESS_DENIED.getCode(), exception.getCode());
        verify(customerUserMapper, never()).selectById(10L);
    }

    @Test
    void shouldShowExpiredPinLockAsUnlocked() {
        allowSuperAdmin();
        CustomerUser customer = customer(CustomerStatus.ACTIVE);
        when(customerUserMapper.selectById(10L)).thenReturn(customer);
        when(customerSecurityMapper.selectById(10L)).thenReturn(CustomerSecurity.builder()
                .customerId(10L)
                .consumePinHash("hash")
                .lockedUntil(NOW.minusSeconds(1))
                .build());

        var result = service.get(5L, 10L);

        assertTrue(result.consumePinConfigured());
        assertFalse(result.consumePinLocked());
    }

    private void allowSuperAdmin() {
        when(sysUserMapper.selectById(5L)).thenReturn(SysUser.builder()
                .id(5L)
                .status(SysUserStatus.ACTIVE)
                .build());
        when(staffAuthorityMapper.selectRoleCodes(5L))
                .thenReturn(List.of(RoleCode.SUPER_ADMIN.getCode()));
    }

    private CustomerUser customer(CustomerStatus status) {
        return CustomerUser.builder()
                .id(10L)
                .phone("13800138000")
                .nickname("测试客户")
                .status(status)
                .tokenVersion(3)
                .createTime(NOW.minusDays(10))
                .updateTime(NOW.minusDays(1))
                .build();
    }
}
