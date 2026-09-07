package com.core.coreboot.platform.recharge.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.core.coreboot.exception.CustomException;
import com.core.coreboot.exception.ExceptionEnum;
import com.core.coreboot.platform.audit.entity.AuditLog;
import com.core.coreboot.platform.audit.mapper.AuditLogMapper;
import com.core.coreboot.platform.common.enums.CustomerStatus;
import com.core.coreboot.platform.common.enums.FundReceiver;
import com.core.coreboot.platform.common.enums.PaymentMethod;
import com.core.coreboot.platform.common.enums.PointLedgerType;
import com.core.coreboot.platform.common.enums.RechargeChannel;
import com.core.coreboot.platform.common.enums.RechargeOrderStatus;
import com.core.coreboot.platform.common.enums.RoleCode;
import com.core.coreboot.platform.customer.entity.CustomerUser;
import com.core.coreboot.platform.customer.mapper.CustomerUserMapper;
import com.core.coreboot.platform.point.entity.PointAccount;
import com.core.coreboot.platform.point.entity.PointLedger;
import com.core.coreboot.platform.point.entity.PointLot;
import com.core.coreboot.platform.point.mapper.PointAccountMapper;
import com.core.coreboot.platform.point.mapper.PointLedgerMapper;
import com.core.coreboot.platform.point.mapper.PointLotMapper;
import com.core.coreboot.platform.recharge.entity.RechargeOrder;
import com.core.coreboot.platform.recharge.mapper.RechargeOrderMapper;
import com.core.coreboot.platform.recharge.model.OfflineRechargeCommand;
import com.core.coreboot.platform.recharge.model.OfflineRechargeResult;
import com.core.coreboot.platform.staff.service.StaffStoreAuthorizationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OfflineRechargeServiceImplTest {
    @Mock
    private CustomerUserMapper customerUserMapper;
    @Mock
    private StaffStoreAuthorizationService staffStoreAuthorizationService;
    @Mock
    private PointAccountMapper pointAccountMapper;
    @Mock
    private RechargeOrderMapper rechargeOrderMapper;
    @Mock
    private PointLotMapper pointLotMapper;
    @Mock
    private PointLedgerMapper pointLedgerMapper;
    @Mock
    private AuditLogMapper auditLogMapper;

    private OfflineRechargeServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new OfflineRechargeServiceImpl(
                customerUserMapper,
                staffStoreAuthorizationService,
                pointAccountMapper,
                rechargeOrderMapper,
                pointLotMapper,
                pointLedgerMapper,
                auditLogMapper,
                new ObjectMapper()
        );
    }

    @Test
    void shouldCompleteOfflineRechargeInOneWriteFlow() {
        OfflineRechargeCommand command = command();
        CustomerUser customer = CustomerUser.builder().id(1L).status(CustomerStatus.ACTIVE).build();
        PointAccount account = PointAccount.builder().id(4L).customerId(1L).availablePoints(20L).version(0).build();

        when(rechargeOrderMapper.selectOne(ArgumentMatchers.<Wrapper<RechargeOrder>>any())).thenReturn(null);
        when(customerUserMapper.selectById(1L)).thenReturn(customer);
        when(staffStoreAuthorizationService.requireActiveStoreAccess(3L, 2L))
                .thenReturn(RoleCode.CLERK);
        when(pointAccountMapper.selectByCustomerIdForUpdate(1L)).thenReturn(account);
        when(rechargeOrderMapper.selectCount(ArgumentMatchers.<Wrapper<RechargeOrder>>any())).thenReturn(0L);
        when(rechargeOrderMapper.insert(any(RechargeOrder.class))).thenAnswer(invocation -> {
            RechargeOrder order = invocation.getArgument(0);
            order.setId(10L);
            return 1;
        });
        when(pointAccountMapper.increaseBalance(4L, 10L)).thenReturn(1);
        when(pointLotMapper.insert(any(PointLot.class))).thenReturn(1);
        when(pointLedgerMapper.insert(any(PointLedger.class))).thenReturn(1);
        when(auditLogMapper.insert(any(AuditLog.class))).thenReturn(1);

        OfflineRechargeResult result = service.recharge(command);

        assertEquals(1_000L, result.amountCent());
        assertEquals(10L, result.rechargePoints());
        assertEquals(RechargeOrderStatus.COMPLETED, result.orderStatus());

        ArgumentCaptor<RechargeOrder> orderCaptor = ArgumentCaptor.forClass(RechargeOrder.class);
        verify(rechargeOrderMapper).insert(orderCaptor.capture());
        RechargeOrder order = orderCaptor.getValue();
        assertEquals(RechargeChannel.OFFLINE, order.getChannel());
        assertEquals(FundReceiver.PLATFORM, order.getFundReceiver());
        assertEquals("PLATFORM-TX-001", order.getPaymentReference());

        ArgumentCaptor<PointLot> lotCaptor = ArgumentCaptor.forClass(PointLot.class);
        verify(pointLotMapper).insert(lotCaptor.capture());
        assertEquals(10L, lotCaptor.getValue().getTotalPoints());
        assertEquals(10L, lotCaptor.getValue().getRemainingPoints());
        assertEquals(10L, lotCaptor.getValue().getSourceRechargeOrderId());

        ArgumentCaptor<PointLedger> ledgerCaptor = ArgumentCaptor.forClass(PointLedger.class);
        verify(pointLedgerMapper).insert(ledgerCaptor.capture());
        assertEquals(10L, ledgerCaptor.getValue().getDeltaPoints());
        assertEquals(30L, ledgerCaptor.getValue().getBalanceAfter());
        assertEquals(PointLedgerType.RECHARGE, ledgerCaptor.getValue().getLedgerType());

        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogMapper).insert(auditCaptor.capture());
        assertEquals("127.0.0.1", auditCaptor.getValue().getClientIp());
    }

    @Test
    void shouldReplayCompletedOrderForSameIdempotencyKey() {
        RechargeOrder existing = RechargeOrder.builder()
                .orderNo("RCH-EXISTING")
                .customerId(1L)
                .rechargeStoreId(2L)
                .operatorId(3L)
                .amountCent(1_000L)
                .rechargePoints(10L)
                .channel(RechargeChannel.OFFLINE)
                .paymentMethod(PaymentMethod.BANK_TRANSFER)
                .fundReceiver(FundReceiver.PLATFORM)
                .paymentReference("PLATFORM-TX-001")
                .idempotencyKey("request-001")
                .remark("柜台核对")
                .orderStatus(RechargeOrderStatus.COMPLETED)
                .build();
        allowActiveClerk();
        when(rechargeOrderMapper.selectOne(ArgumentMatchers.<Wrapper<RechargeOrder>>any())).thenReturn(existing);

        OfflineRechargeResult result = service.recharge(command());

        assertEquals("RCH-EXISTING", result.orderNo());
        verify(pointAccountMapper, never()).selectByCustomerIdForUpdate(any());
        verify(pointLedgerMapper, never()).insert(any(PointLedger.class));
    }

    @Test
    void shouldRejectIdempotencyKeyReusedWithDifferentAmount() {
        RechargeOrder existing = RechargeOrder.builder()
                .customerId(1L)
                .rechargeStoreId(2L)
                .operatorId(3L)
                .amountCent(2_000L)
                .rechargePoints(20L)
                .channel(RechargeChannel.OFFLINE)
                .paymentMethod(PaymentMethod.BANK_TRANSFER)
                .fundReceiver(FundReceiver.PLATFORM)
                .paymentReference("PLATFORM-TX-001")
                .idempotencyKey("request-001")
                .remark("柜台核对")
                .orderStatus(RechargeOrderStatus.COMPLETED)
                .build();
        allowActiveClerk();
        when(rechargeOrderMapper.selectOne(ArgumentMatchers.<Wrapper<RechargeOrder>>any())).thenReturn(existing);

        CustomException exception = assertThrows(CustomException.class, () -> service.recharge(command()));

        assertEquals(ExceptionEnum.PLATFORM_IDEMPOTENCY_CONFLICT.getCode(), exception.getCode());
    }

    @Test
    void shouldRejectNonWholeYuanAmountBeforeDatabaseAccess() {
        OfflineRechargeCommand command = new OfflineRechargeCommand(
                1L, 2L, 3L, 199L, PaymentMethod.BANK_TRANSFER,
                "PLATFORM-TX-001", "request-001", null, "127.0.0.1"
        );

        CustomException exception = assertThrows(CustomException.class, () -> service.recharge(command));

        assertEquals(ExceptionEnum.PLATFORM_RECHARGE_AMOUNT_INVALID.getCode(), exception.getCode());
        verify(rechargeOrderMapper, never()).selectOne(ArgumentMatchers.<Wrapper<RechargeOrder>>any());
    }

    @Test
    void shouldRejectOperatorWithoutStoreAccess() {
        when(customerUserMapper.selectById(1L))
                .thenReturn(CustomerUser.builder().id(1L).status(CustomerStatus.ACTIVE).build());
        when(staffStoreAuthorizationService.requireActiveStoreAccess(3L, 2L))
                .thenThrow(new CustomException(ExceptionEnum.PLATFORM_STORE_ACCESS_DENIED));

        CustomException exception = assertThrows(CustomException.class, () -> service.recharge(command()));

        assertEquals(ExceptionEnum.PLATFORM_STORE_ACCESS_DENIED.getCode(), exception.getCode());
        verify(pointAccountMapper, never()).ensureAccount(any());
    }

    private void allowActiveClerk() {
        when(customerUserMapper.selectById(1L))
                .thenReturn(CustomerUser.builder().id(1L).status(CustomerStatus.ACTIVE).build());
        when(staffStoreAuthorizationService.requireActiveStoreAccess(3L, 2L))
                .thenReturn(RoleCode.CLERK);
    }

    private OfflineRechargeCommand command() {
        return new OfflineRechargeCommand(
                1L,
                2L,
                3L,
                1_000L,
                PaymentMethod.BANK_TRANSFER,
                "PLATFORM-TX-001",
                "request-001",
                "柜台核对",
                "127.0.0.1"
        );
    }
}
