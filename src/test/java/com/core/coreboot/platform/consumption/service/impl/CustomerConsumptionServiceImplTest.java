package com.core.coreboot.platform.consumption.service.impl;

import com.core.coreboot.exception.CustomException;
import com.core.coreboot.exception.ExceptionEnum;
import com.core.coreboot.platform.common.enums.ConsumptionOrderStatus;
import com.core.coreboot.platform.consumption.entity.ConsumptionOrder;
import com.core.coreboot.platform.consumption.mapper.ConsumptionOrderMapper;
import com.core.coreboot.platform.consumption.model.ConsumptionConfirmationOutcome;
import com.core.coreboot.platform.consumption.model.ConsumptionConfirmationResult;
import com.core.coreboot.platform.consumption.model.CustomerPendingConsumptionView;
import com.core.coreboot.platform.customer.service.CustomerConsumePinService;
import com.core.coreboot.platform.merchant.entity.Store;
import com.core.coreboot.platform.merchant.mapper.StoreMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerConsumptionServiceImplTest {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 8, 12, 0);

    @Mock
    private ConsumptionOrderMapper consumptionOrderMapper;
    @Mock
    private StoreMapper storeMapper;
    @Mock
    private CustomerConsumePinService customerConsumePinService;
    @Mock
    private ConsumptionConfirmationTransactionService confirmationTransactionService;

    private CustomerConsumptionServiceImpl service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-09-08T12:00:00Z"), ZoneOffset.UTC);
        service = new CustomerConsumptionServiceImpl(
                consumptionOrderMapper,
                storeMapper,
                customerConsumePinService,
                confirmationTransactionService,
                clock
        );
    }

    @Test
    void shouldExpireStaleOrdersAndReturnCurrentPendingOrder() {
        ConsumptionOrder order = pendingOrder(NOW.plusMinutes(5));
        when(consumptionOrderMapper.selectActivePendingByCustomerId(1L)).thenReturn(order);
        when(storeMapper.selectById(2L)).thenReturn(Store.builder().id(2L).storeName("测试门店").build());

        CustomerPendingConsumptionView result = service.getPending(1L);

        assertEquals("CSM123", result.orderNo());
        assertEquals("测试门店", result.storeName());
        assertEquals(100L, result.consumePoints());
        verify(consumptionOrderMapper).expirePendingByCustomerId(1L, NOW);
    }

    @Test
    void shouldReturnNullWhenThereIsNoPendingOrder() {
        assertNull(service.getPending(1L));

        verify(consumptionOrderMapper).expirePendingByCustomerId(1L, NOW);
        verify(storeMapper, never()).selectById(2L);
    }

    @Test
    void shouldVerifyPinBeforeConfirmingPendingOrder() {
        ConsumptionOrder order = pendingOrder(NOW.plusMinutes(5));
        ConsumptionConfirmationResult result = completedResult();
        when(consumptionOrderMapper.selectByOrderNo("CSM123")).thenReturn(order);
        when(confirmationTransactionService.confirmOrReplay(1L, "CSM123", "192.0.2.51"))
                .thenReturn(ConsumptionConfirmationOutcome.completed(result));

        ConsumptionConfirmationResult actual = service.confirm(
                1L,
                " CSM123 ",
                "258369",
                "192.0.2.51"
        );

        assertEquals(result, actual);
        verify(customerConsumePinService).verifyPin(1L, "258369", "192.0.2.51");
        verify(confirmationTransactionService).confirmOrReplay(1L, "CSM123", "192.0.2.51");
    }

    @Test
    void shouldExpireWithoutCheckingPinWhenSnapshotHasExpired() {
        ConsumptionOrder order = pendingOrder(NOW);
        when(consumptionOrderMapper.selectByOrderNo("CSM123")).thenReturn(order);
        when(confirmationTransactionService.confirmOrReplay(1L, "CSM123", null))
                .thenReturn(ConsumptionConfirmationOutcome.expiredOrder());

        CustomException exception = assertThrows(
                CustomException.class,
                () -> service.confirm(1L, "CSM123", "258369", null)
        );

        assertEquals(ExceptionEnum.PLATFORM_CONSUMPTION_ORDER_EXPIRED.getCode(), exception.getCode());
        verify(customerConsumePinService, never()).verifyPin(1L, "258369", null);
    }

    @Test
    void shouldReplayCompletedOrderWithoutCheckingPin() {
        ConsumptionOrder order = pendingOrder(NOW.minusMinutes(1));
        order.setOrderStatus(ConsumptionOrderStatus.COMPLETED);
        when(consumptionOrderMapper.selectByOrderNo("CSM123")).thenReturn(order);
        when(confirmationTransactionService.confirmOrReplay(1L, "CSM123", null))
                .thenReturn(ConsumptionConfirmationOutcome.completed(completedResult()));

        ConsumptionConfirmationResult result = service.confirm(1L, "CSM123", "", null);

        assertEquals(ConsumptionOrderStatus.COMPLETED, result.orderStatus());
        verify(customerConsumePinService, never()).verifyPin(1L, "", null);
    }

    @Test
    void shouldHideWhetherAnotherCustomersOrderExists() {
        ConsumptionOrder order = pendingOrder(NOW.plusMinutes(5));
        order.setCustomerId(99L);
        when(consumptionOrderMapper.selectByOrderNo("CSM123")).thenReturn(order);

        CustomException exception = assertThrows(
                CustomException.class,
                () -> service.confirm(1L, "CSM123", "258369", null)
        );

        assertEquals(ExceptionEnum.PLATFORM_CONSUMPTION_ORDER_NOT_FOUND.getCode(), exception.getCode());
        verify(customerConsumePinService, never()).verifyPin(1L, "258369", null);
    }

    private ConsumptionOrder pendingOrder(LocalDateTime expiresTime) {
        return ConsumptionOrder.builder()
                .id(20L)
                .orderNo("CSM123")
                .customerId(1L)
                .storeId(2L)
                .consumePoints(100L)
                .grossAmountCent(10_000L)
                .expiresTime(expiresTime)
                .orderStatus(ConsumptionOrderStatus.PENDING_CONFIRM)
                .remark("现场消费")
                .createTime(NOW.minusMinutes(1))
                .build();
    }

    private ConsumptionConfirmationResult completedResult() {
        return new ConsumptionConfirmationResult(
                "CSM123",
                2L,
                "测试门店",
                100L,
                10_000L,
                50L,
                ConsumptionOrderStatus.COMPLETED,
                NOW
        );
    }
}
