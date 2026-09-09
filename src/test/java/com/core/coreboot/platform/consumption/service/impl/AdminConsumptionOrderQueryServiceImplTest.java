package com.core.coreboot.platform.consumption.service.impl;

import com.core.coreboot.exception.CustomException;
import com.core.coreboot.exception.ExceptionEnum;
import com.core.coreboot.platform.common.enums.ConsumptionOrderStatus;
import com.core.coreboot.platform.common.enums.ConsumptionVerificationMode;
import com.core.coreboot.platform.common.enums.RoleCode;
import com.core.coreboot.platform.consumption.entity.ConsumptionOrder;
import com.core.coreboot.platform.consumption.mapper.ConsumptionOrderMapper;
import com.core.coreboot.platform.staff.service.StaffStoreAuthorizationService;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminConsumptionOrderQueryServiceImplTest {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 10, 2, 0);

    @Mock
    private ConsumptionOrderMapper consumptionOrderMapper;
    @Mock
    private StaffStoreAuthorizationService staffStoreAuthorizationService;

    private AdminConsumptionOrderQueryServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AdminConsumptionOrderQueryServiceImpl(
                consumptionOrderMapper,
                staffStoreAuthorizationService,
                Clock.fixed(Instant.parse("2026-09-10T02:00:00Z"), ZoneOffset.UTC)
        );
    }

    @Test
    void shouldReturnOrderAfterCheckingStoreAccess() {
        ConsumptionOrder order = order(ConsumptionOrderStatus.COMPLETED, NOW.plusMinutes(1));
        order.setCompletedTime(NOW.minusSeconds(5));
        when(consumptionOrderMapper.selectByOrderNo("CO20260910001")).thenReturn(order);
        when(staffStoreAuthorizationService.requireActiveStoreAccess(9L, 2L))
                .thenReturn(RoleCode.CLERK);

        var result = service.getStatus(9L, " CO20260910001 ");

        assertEquals(ConsumptionOrderStatus.COMPLETED, result.orderStatus());
        assertEquals(100L, result.consumePoints());
        verify(staffStoreAuthorizationService).requireActiveStoreAccess(9L, 2L);
        verify(consumptionOrderMapper, never()).expirePendingById(7L, NOW);
    }

    @Test
    void shouldExpireOverduePendingOrderBeforeReturning() {
        ConsumptionOrder pending = order(ConsumptionOrderStatus.PENDING_CONFIRM, NOW.minusSeconds(1));
        ConsumptionOrder expired = order(ConsumptionOrderStatus.EXPIRED, NOW.minusSeconds(1));
        when(consumptionOrderMapper.selectByOrderNo("CO20260910001"))
                .thenReturn(pending, expired);
        when(staffStoreAuthorizationService.requireActiveStoreAccess(9L, 2L))
                .thenReturn(RoleCode.CLERK);
        when(consumptionOrderMapper.expirePendingById(7L, NOW)).thenReturn(1);

        var result = service.getStatus(9L, "CO20260910001");

        assertEquals(ConsumptionOrderStatus.EXPIRED, result.orderStatus());
        verify(consumptionOrderMapper).expirePendingById(7L, NOW);
    }

    @Test
    void shouldRejectUnknownOrderWithoutCheckingAnyStore() {
        when(consumptionOrderMapper.selectByOrderNo("UNKNOWN")).thenReturn(null);

        CustomException exception = assertThrows(
                CustomException.class,
                () -> service.getStatus(9L, "UNKNOWN")
        );

        assertEquals(ExceptionEnum.PLATFORM_CONSUMPTION_ORDER_NOT_FOUND.getCode(), exception.getCode());
        verify(staffStoreAuthorizationService, never()).requireActiveStoreAccess(9L, 2L);
    }

    private ConsumptionOrder order(ConsumptionOrderStatus status, LocalDateTime expiresTime) {
        return ConsumptionOrder.builder()
                .id(7L)
                .orderNo("CO20260910001")
                .customerId(5L)
                .storeId(2L)
                .consumePoints(100L)
                .grossAmountCent(10_000L)
                .verificationMode(ConsumptionVerificationMode.CUSTOMER_PIN)
                .expiresTime(expiresTime)
                .orderStatus(status)
                .createTime(NOW.minusMinutes(1))
                .build();
    }
}
