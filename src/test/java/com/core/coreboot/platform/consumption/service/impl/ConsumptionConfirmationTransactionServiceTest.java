package com.core.coreboot.platform.consumption.service.impl;

import com.core.coreboot.exception.CustomException;
import com.core.coreboot.exception.ExceptionEnum;
import com.core.coreboot.platform.audit.entity.AuditLog;
import com.core.coreboot.platform.audit.mapper.AuditLogMapper;
import com.core.coreboot.platform.common.enums.ConsumptionOrderStatus;
import com.core.coreboot.platform.common.enums.ConsumptionVerificationMode;
import com.core.coreboot.platform.common.enums.PointLedgerBusinessType;
import com.core.coreboot.platform.common.enums.PointLedgerType;
import com.core.coreboot.platform.common.enums.StoreStatus;
import com.core.coreboot.platform.consumption.entity.ConsumptionOrder;
import com.core.coreboot.platform.consumption.mapper.ConsumptionOrderMapper;
import com.core.coreboot.platform.consumption.model.ConsumptionConfirmationOutcome;
import com.core.coreboot.platform.merchant.entity.Store;
import com.core.coreboot.platform.merchant.mapper.StoreMapper;
import com.core.coreboot.platform.point.entity.PointAccount;
import com.core.coreboot.platform.point.entity.PointLedger;
import com.core.coreboot.platform.point.entity.PointLot;
import com.core.coreboot.platform.point.entity.PointLotUsage;
import com.core.coreboot.platform.point.mapper.PointAccountMapper;
import com.core.coreboot.platform.point.mapper.PointLedgerMapper;
import com.core.coreboot.platform.point.mapper.PointLotMapper;
import com.core.coreboot.platform.point.mapper.PointLotUsageMapper;
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
class ConsumptionConfirmationTransactionServiceTest {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 8, 12, 0);

    @Mock
    private PointAccountMapper pointAccountMapper;
    @Mock
    private ConsumptionOrderMapper consumptionOrderMapper;
    @Mock
    private StoreMapper storeMapper;
    @Mock
    private PointLotMapper pointLotMapper;
    @Mock
    private PointLotUsageMapper pointLotUsageMapper;
    @Mock
    private PointLedgerMapper pointLedgerMapper;
    @Mock
    private AuditLogMapper auditLogMapper;

    private ConsumptionConfirmationTransactionService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-09-08T12:00:00Z"), ZoneOffset.UTC);
        service = new ConsumptionConfirmationTransactionService(
                pointAccountMapper,
                consumptionOrderMapper,
                storeMapper,
                pointLotMapper,
                pointLotUsageMapper,
                pointLedgerMapper,
                auditLogMapper,
                new ObjectMapper(),
                clock
        );
    }

    @Test
    void shouldDeductAccountAndFifoLotsThenCompleteOrder() {
        allowAccountOrderAndStore(150L, pendingOrder(NOW.plusMinutes(5)), StoreStatus.ACTIVE);
        when(pointLotMapper.selectAvailableByCustomerIdForUpdate(1L)).thenReturn(List.of(
                lot(100L, 60L),
                lot(101L, 90L)
        ));
        when(pointLotMapper.consumePoints(any(), any(), any())).thenReturn(1);
        when(pointLotUsageMapper.insert(any(PointLotUsage.class))).thenReturn(1);
        when(pointAccountMapper.decreaseBalance(9L, 100L)).thenReturn(1);
        when(pointLedgerMapper.insert(any(PointLedger.class))).thenReturn(1);
        when(consumptionOrderMapper.completePendingById(20L, NOW)).thenReturn(1);
        when(auditLogMapper.insert(any(AuditLog.class))).thenReturn(1);

        ConsumptionConfirmationOutcome outcome = service.confirmOrReplay(
                1L,
                "CSM123",
                "192.0.2.50"
        );

        assertFalse(outcome.expired());
        assertEquals(ConsumptionOrderStatus.COMPLETED, outcome.result().orderStatus());
        assertEquals(50L, outcome.result().availablePoints());
        assertEquals(NOW, outcome.result().completedTime());
        verify(pointLotMapper).consumePoints(100L, 1L, 60L);
        verify(pointLotMapper).consumePoints(101L, 1L, 40L);

        ArgumentCaptor<PointLotUsage> usageCaptor = ArgumentCaptor.forClass(PointLotUsage.class);
        verify(pointLotUsageMapper, org.mockito.Mockito.times(2)).insert(usageCaptor.capture());
        assertEquals(List.of(60L, 40L), usageCaptor.getAllValues().stream()
                .map(PointLotUsage::getUsedPoints)
                .toList());

        ArgumentCaptor<PointLedger> ledgerCaptor = ArgumentCaptor.forClass(PointLedger.class);
        verify(pointLedgerMapper).insert(ledgerCaptor.capture());
        assertEquals(-100L, ledgerCaptor.getValue().getDeltaPoints());
        assertEquals(50L, ledgerCaptor.getValue().getBalanceAfter());
        assertEquals(PointLedgerType.CONSUME, ledgerCaptor.getValue().getLedgerType());
        assertEquals(PointLedgerBusinessType.CONSUMPTION_ORDER, ledgerCaptor.getValue().getBusinessType());
        assertEquals("CSM123", ledgerCaptor.getValue().getBusinessNo());

        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogMapper).insert(auditCaptor.capture());
        assertEquals("CONSUMPTION_ORDER_CONFIRMED", auditCaptor.getValue().getAction());
        assertEquals("192.0.2.50", auditCaptor.getValue().getClientIp());
        assertFalse(auditCaptor.getValue().getAfterSnapshot().contains("258369"));
    }

    @Test
    void shouldExpireOrderWithoutDeductingPoints() {
        allowAccountOrderAndStore(150L, pendingOrder(NOW), StoreStatus.ACTIVE);
        when(consumptionOrderMapper.expirePendingById(20L, NOW)).thenReturn(1);

        ConsumptionConfirmationOutcome outcome = service.confirmOrReplay(1L, "CSM123", null);

        assertTrue(outcome.expired());
        verify(pointLotMapper, never()).selectAvailableByCustomerIdForUpdate(any());
        verify(pointAccountMapper, never()).decreaseBalance(any(), any());
        verify(pointLedgerMapper, never()).insert(any(PointLedger.class));
    }

    @Test
    void shouldReplayCompletedOrderWithoutAnotherDeduction() {
        ConsumptionOrder order = pendingOrder(NOW.plusMinutes(5));
        order.setOrderStatus(ConsumptionOrderStatus.COMPLETED);
        order.setCompletedTime(NOW.minusMinutes(1));
        allowAccountOrderAndStore(50L, order, StoreStatus.SUSPENDED);

        ConsumptionConfirmationOutcome outcome = service.confirmOrReplay(1L, "CSM123", null);

        assertFalse(outcome.expired());
        assertEquals(50L, outcome.result().availablePoints());
        verify(pointLotMapper, never()).selectAvailableByCustomerIdForUpdate(any());
        verify(pointAccountMapper, never()).decreaseBalance(any(), any());
    }

    @Test
    void shouldRejectInsufficientCurrentBalance() {
        allowAccountOrderAndStore(99L, pendingOrder(NOW.plusMinutes(5)), StoreStatus.ACTIVE);

        CustomException exception = assertThrows(
                CustomException.class,
                () -> service.confirmOrReplay(1L, "CSM123", null)
        );

        assertEquals(ExceptionEnum.PLATFORM_POINT_BALANCE_INSUFFICIENT.getCode(), exception.getCode());
        verify(pointLotMapper, never()).selectAvailableByCustomerIdForUpdate(any());
        verify(pointAccountMapper, never()).decreaseBalance(any(), any());
    }

    @Test
    void shouldRejectInconsistentLotBalanceBeforeWriting() {
        allowAccountOrderAndStore(150L, pendingOrder(NOW.plusMinutes(5)), StoreStatus.ACTIVE);
        when(pointLotMapper.selectAvailableByCustomerIdForUpdate(1L))
                .thenReturn(List.of(lot(100L, 99L)));

        CustomException exception = assertThrows(
                CustomException.class,
                () -> service.confirmOrReplay(1L, "CSM123", null)
        );

        assertEquals(ExceptionEnum.PLATFORM_POINT_ACCOUNT_ERROR.getCode(), exception.getCode());
        verify(pointLotMapper, never()).consumePoints(any(), any(), any());
        verify(pointAccountMapper, never()).decreaseBalance(any(), any());
    }

    private void allowAccountOrderAndStore(
            long availablePoints,
            ConsumptionOrder order,
            StoreStatus storeStatus
    ) {
        when(pointAccountMapper.selectByCustomerIdForUpdate(1L)).thenReturn(PointAccount.builder()
                .id(9L)
                .customerId(1L)
                .availablePoints(availablePoints)
                .build());
        when(consumptionOrderMapper.selectByOrderNoForUpdate("CSM123")).thenReturn(order);
        when(storeMapper.selectByIdForUpdate(2L)).thenReturn(Store.builder()
                .id(2L)
                .storeName("测试门店")
                .status(storeStatus)
                .build());
    }

    private ConsumptionOrder pendingOrder(LocalDateTime expiresTime) {
        return ConsumptionOrder.builder()
                .id(20L)
                .orderNo("CSM123")
                .customerId(1L)
                .storeId(2L)
                .consumePoints(100L)
                .grossAmountCent(10_000L)
                .platformFeeCent(500L)
                .storePayableCent(9_500L)
                .verificationMode(ConsumptionVerificationMode.CUSTOMER_PIN)
                .expiresTime(expiresTime)
                .orderStatus(ConsumptionOrderStatus.PENDING_CONFIRM)
                .operatorId(3L)
                .remark("现场消费")
                .build();
    }

    private PointLot lot(long id, long remainingPoints) {
        return PointLot.builder()
                .id(id)
                .customerId(1L)
                .remainingPoints(remainingPoints)
                .build();
    }
}
