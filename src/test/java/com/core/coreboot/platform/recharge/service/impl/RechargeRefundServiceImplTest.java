package com.core.coreboot.platform.recharge.service.impl;

import com.core.coreboot.exception.CustomException;
import com.core.coreboot.exception.ExceptionEnum;
import com.core.coreboot.platform.audit.entity.AuditLog;
import com.core.coreboot.platform.audit.mapper.AuditLogMapper;
import com.core.coreboot.platform.common.enums.PointLedgerBusinessType;
import com.core.coreboot.platform.common.enums.PointLedgerType;
import com.core.coreboot.platform.common.enums.PointLotStatus;
import com.core.coreboot.platform.common.enums.RechargeOrderStatus;
import com.core.coreboot.platform.common.enums.RechargeRefundStatus;
import com.core.coreboot.platform.common.enums.RefundMethod;
import com.core.coreboot.platform.common.enums.RoleCode;
import com.core.coreboot.platform.common.enums.SysUserStatus;
import com.core.coreboot.platform.point.entity.PointAccount;
import com.core.coreboot.platform.point.entity.PointLedger;
import com.core.coreboot.platform.point.entity.PointLot;
import com.core.coreboot.platform.point.mapper.PointAccountMapper;
import com.core.coreboot.platform.point.mapper.PointLedgerMapper;
import com.core.coreboot.platform.point.mapper.PointLotMapper;
import com.core.coreboot.platform.point.mapper.PointLotUsageMapper;
import com.core.coreboot.platform.recharge.entity.RechargeOrder;
import com.core.coreboot.platform.recharge.entity.RechargeRefund;
import com.core.coreboot.platform.recharge.mapper.RechargeOrderMapper;
import com.core.coreboot.platform.recharge.mapper.RechargeRefundMapper;
import com.core.coreboot.platform.recharge.model.RechargeRefundCommand;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RechargeRefundServiceImplTest {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 8, 18, 0);

    @Mock
    private RechargeOrderMapper rechargeOrderMapper;
    @Mock
    private RechargeRefundMapper rechargeRefundMapper;
    @Mock
    private PointAccountMapper pointAccountMapper;
    @Mock
    private PointLotMapper pointLotMapper;
    @Mock
    private PointLotUsageMapper pointLotUsageMapper;
    @Mock
    private PointLedgerMapper pointLedgerMapper;
    @Mock
    private SysUserMapper sysUserMapper;
    @Mock
    private StaffAuthorityMapper staffAuthorityMapper;
    @Mock
    private AuditLogMapper auditLogMapper;

    private RechargeRefundServiceImpl service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-09-08T18:00:00Z"), ZoneOffset.UTC);
        service = new RechargeRefundServiceImpl(
                rechargeOrderMapper,
                rechargeRefundMapper,
                pointAccountMapper,
                pointLotMapper,
                pointLotUsageMapper,
                pointLedgerMapper,
                sysUserMapper,
                staffAuthorityMapper,
                auditLogMapper,
                new ObjectMapper(),
                clock
        );
    }

    @Test
    void shouldRefundEntireUntouchedRechargeInOneTransaction() {
        allowSuperAdmin();
        RechargeOrder order = completedOrder();
        PointAccount account = account(1_000L);
        PointLot lot = untouchedLot();
        when(rechargeOrderMapper.selectByOrderNo("RCH100")).thenReturn(order);
        when(pointAccountMapper.selectByCustomerIdForUpdate(7L)).thenReturn(account);
        when(rechargeOrderMapper.selectByOrderNoForUpdate("RCH100")).thenReturn(order);
        when(pointLotMapper.selectByRechargeOrderIdForUpdate(10L)).thenReturn(lot);
        when(rechargeRefundMapper.insert(any(RechargeRefund.class))).thenReturn(1);
        when(pointLotMapper.markUntouchedLotRefunded(9L, 7L)).thenReturn(1);
        when(pointAccountMapper.decreaseBalance(8L, 1_000L)).thenReturn(1);
        when(rechargeOrderMapper.markRefunded(10L)).thenReturn(1);
        when(pointLedgerMapper.insert(any(PointLedger.class))).thenReturn(1);
        when(auditLogMapper.insert(any(AuditLog.class))).thenReturn(1);

        var result = service.refund(command());

        assertEquals("RCH100", result.rechargeOrderNo());
        assertEquals(1_000L, result.refundPoints());
        assertEquals(100_000L, result.refundAmountCent());
        assertEquals(RechargeRefundStatus.COMPLETED, result.refundStatus());
        assertEquals(0L, result.availablePoints());
        assertEquals(NOW, result.completedTime());

        ArgumentCaptor<RechargeRefund> refundCaptor = ArgumentCaptor.forClass(RechargeRefund.class);
        verify(rechargeRefundMapper).insert(refundCaptor.capture());
        assertEquals(10L, refundCaptor.getValue().getRechargeOrderId());
        assertEquals(RefundMethod.BANK_TRANSFER, refundCaptor.getValue().getRefundMethod());
        assertEquals("REFUND-TX-100", refundCaptor.getValue().getRefundReference());

        ArgumentCaptor<PointLedger> ledgerCaptor = ArgumentCaptor.forClass(PointLedger.class);
        verify(pointLedgerMapper).insert(ledgerCaptor.capture());
        assertEquals(-1_000L, ledgerCaptor.getValue().getDeltaPoints());
        assertEquals(PointLedgerType.REFUND, ledgerCaptor.getValue().getLedgerType());
        assertEquals(PointLedgerBusinessType.RECHARGE_REFUND, ledgerCaptor.getValue().getBusinessType());

        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogMapper).insert(auditCaptor.capture());
        assertEquals(RoleCode.SUPER_ADMIN, auditCaptor.getValue().getOperatorRole());
        assertEquals("RECHARGE_REFUND_COMPLETED", auditCaptor.getValue().getAction());
    }

    @Test
    void shouldRejectRechargeWhoseLotHasEverBeenUsed() {
        allowSuperAdmin();
        allowOrderAccountAndLot(account(1_000L), untouchedLot());
        when(pointLotUsageMapper.countByPointLotId(9L)).thenReturn(1L);

        CustomException exception = assertThrows(CustomException.class, () -> service.refund(command()));

        assertEquals(ExceptionEnum.PLATFORM_RECHARGE_POINTS_ALREADY_USED.getCode(), exception.getCode());
        verify(rechargeRefundMapper, never()).insert(any(RechargeRefund.class));
    }

    @Test
    void shouldRejectPartiallyConsumedPointLot() {
        allowSuperAdmin();
        PointLot partialLot = untouchedLot();
        partialLot.setRemainingPoints(900L);
        allowOrderAccountAndLot(account(1_000L), partialLot);

        CustomException exception = assertThrows(CustomException.class, () -> service.refund(command()));

        assertEquals(ExceptionEnum.PLATFORM_RECHARGE_POINTS_ALREADY_USED.getCode(), exception.getCode());
        verify(pointLotUsageMapper, never()).countByPointLotId(any());
    }

    @Test
    void shouldRejectRefundWhenCurrentBalanceIsInsufficient() {
        allowSuperAdmin();
        allowOrderAccountAndLot(account(999L), untouchedLot());

        CustomException exception = assertThrows(CustomException.class, () -> service.refund(command()));

        assertEquals(ExceptionEnum.PLATFORM_POINT_BALANCE_INSUFFICIENT.getCode(), exception.getCode());
        verify(rechargeRefundMapper, never()).insert(any(RechargeRefund.class));
    }

    @Test
    void shouldReplayCompletedRefundWithoutSecondDeduction() {
        allowSuperAdmin();
        RechargeRefund existing = completedRefund();
        RechargeOrder refundedOrder = completedOrder();
        refundedOrder.setOrderStatus(RechargeOrderStatus.REFUNDED);
        when(rechargeRefundMapper.selectByIdempotencyKey("refund-request-100"))
                .thenReturn(existing);
        when(pointAccountMapper.selectByCustomerId(7L)).thenReturn(account(0L));
        when(rechargeOrderMapper.selectById(10L)).thenReturn(refundedOrder);

        var result = service.refund(command());

        assertEquals("RFD100", result.refundNo());
        assertEquals(0L, result.availablePoints());
        verify(pointAccountMapper, never()).selectByCustomerIdForUpdate(any());
        verify(pointAccountMapper, never()).decreaseBalance(any(), any());
    }

    @Test
    void shouldRejectDifferentRequestUsingSameIdempotencyKey() {
        allowSuperAdmin();
        RechargeRefund existing = completedRefund();
        RechargeOrder refundedOrder = completedOrder();
        refundedOrder.setOrderStatus(RechargeOrderStatus.REFUNDED);
        when(rechargeRefundMapper.selectByIdempotencyKey("refund-request-100"))
                .thenReturn(existing);
        when(pointAccountMapper.selectByCustomerId(7L)).thenReturn(account(0L));
        when(rechargeOrderMapper.selectById(10L)).thenReturn(refundedOrder);
        RechargeRefundCommand changed = new RechargeRefundCommand(
                "RCH100",
                3L,
                RefundMethod.BANK_TRANSFER,
                "REFUND-TX-100",
                "不同原因",
                "refund-request-100",
                "192.0.2.30"
        );

        CustomException exception = assertThrows(CustomException.class, () -> service.refund(changed));

        assertEquals(ExceptionEnum.PLATFORM_IDEMPOTENCY_CONFLICT.getCode(), exception.getCode());
    }

    @Test
    void shouldRejectUsedRefundReference() {
        allowSuperAdmin();
        allowOrderAccountAndLot(account(1_000L), untouchedLot());
        when(rechargeRefundMapper.countByRefundReference(
                RefundMethod.BANK_TRANSFER,
                "REFUND-TX-100"
        )).thenReturn(1L);

        CustomException exception = assertThrows(CustomException.class, () -> service.refund(command()));

        assertEquals(ExceptionEnum.PLATFORM_REFUND_REFERENCE_USED.getCode(), exception.getCode());
        verify(rechargeRefundMapper, never()).insert(any(RechargeRefund.class));
    }

    @Test
    void shouldEnforceSuperAdminInsideDomainService() {
        when(sysUserMapper.selectById(3L)).thenReturn(SysUser.builder()
                .id(3L)
                .status(SysUserStatus.ACTIVE)
                .build());
        when(staffAuthorityMapper.selectRoleCodes(3L)).thenReturn(List.of(RoleCode.STORE_MANAGER.getCode()));

        CustomException exception = assertThrows(CustomException.class, () -> service.refund(command()));

        assertEquals(ExceptionEnum.PLATFORM_ADMIN_ACCESS_DENIED.getCode(), exception.getCode());
        verify(rechargeOrderMapper, never()).selectByOrderNo(any());
    }

    private void allowSuperAdmin() {
        when(sysUserMapper.selectById(3L)).thenReturn(SysUser.builder()
                .id(3L)
                .status(SysUserStatus.ACTIVE)
                .build());
        when(staffAuthorityMapper.selectRoleCodes(3L)).thenReturn(List.of(RoleCode.SUPER_ADMIN.getCode()));
    }

    private void allowOrderAccountAndLot(PointAccount account, PointLot lot) {
        RechargeOrder order = completedOrder();
        when(rechargeOrderMapper.selectByOrderNo("RCH100")).thenReturn(order);
        when(pointAccountMapper.selectByCustomerIdForUpdate(7L)).thenReturn(account);
        when(rechargeOrderMapper.selectByOrderNoForUpdate("RCH100")).thenReturn(order);
        when(pointLotMapper.selectByRechargeOrderIdForUpdate(10L)).thenReturn(lot);
    }

    private RechargeRefundCommand command() {
        return new RechargeRefundCommand(
                "RCH100",
                3L,
                RefundMethod.BANK_TRANSFER,
                "REFUND-TX-100",
                "客户申请退回未消费充值",
                "refund-request-100",
                "192.0.2.30"
        );
    }

    private RechargeOrder completedOrder() {
        return RechargeOrder.builder()
                .id(10L)
                .orderNo("RCH100")
                .customerId(7L)
                .rechargeStoreId(2L)
                .rechargePoints(1_000L)
                .amountCent(100_000L)
                .orderStatus(RechargeOrderStatus.COMPLETED)
                .build();
    }

    private PointAccount account(long availablePoints) {
        return PointAccount.builder()
                .id(8L)
                .customerId(7L)
                .availablePoints(availablePoints)
                .build();
    }

    private PointLot untouchedLot() {
        return PointLot.builder()
                .id(9L)
                .customerId(7L)
                .sourceRechargeOrderId(10L)
                .totalPoints(1_000L)
                .remainingPoints(1_000L)
                .lotStatus(PointLotStatus.AVAILABLE)
                .build();
    }

    private RechargeRefund completedRefund() {
        return RechargeRefund.builder()
                .id(12L)
                .refundNo("RFD100")
                .rechargeOrderId(10L)
                .customerId(7L)
                .refundPoints(1_000L)
                .refundAmountCent(100_000L)
                .refundMethod(RefundMethod.BANK_TRANSFER)
                .refundReference("REFUND-TX-100")
                .refundStatus(RechargeRefundStatus.COMPLETED)
                .operatorId(3L)
                .reason("客户申请退回未消费充值")
                .completedTime(NOW)
                .idempotencyKey("refund-request-100")
                .build();
    }
}
