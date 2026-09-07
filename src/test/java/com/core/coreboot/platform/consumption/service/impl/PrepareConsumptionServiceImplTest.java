package com.core.coreboot.platform.consumption.service.impl;

import com.core.coreboot.exception.CustomException;
import com.core.coreboot.exception.ExceptionEnum;
import com.core.coreboot.platform.audit.entity.AuditLog;
import com.core.coreboot.platform.audit.mapper.AuditLogMapper;
import com.core.coreboot.platform.common.enums.ConsumptionOrderStatus;
import com.core.coreboot.platform.common.enums.ConsumptionVerificationMode;
import com.core.coreboot.platform.common.enums.CustomerStatus;
import com.core.coreboot.platform.common.enums.RoleCode;
import com.core.coreboot.platform.consumption.config.ConsumptionProperties;
import com.core.coreboot.platform.consumption.entity.ConsumptionOrder;
import com.core.coreboot.platform.consumption.mapper.ConsumptionOrderMapper;
import com.core.coreboot.platform.consumption.model.PrepareConsumptionCommand;
import com.core.coreboot.platform.consumption.model.PrepareConsumptionResult;
import com.core.coreboot.platform.customer.entity.CustomerUser;
import com.core.coreboot.platform.customer.mapper.CustomerUserMapper;
import com.core.coreboot.platform.point.entity.PointAccount;
import com.core.coreboot.platform.point.mapper.PointAccountMapper;
import com.core.coreboot.platform.staff.service.StaffStoreAuthorizationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PrepareConsumptionServiceImplTest {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 5, 12, 0);

    @Mock
    private CustomerUserMapper customerUserMapper;
    @Mock
    private StaffStoreAuthorizationService staffStoreAuthorizationService;
    @Mock
    private PointAccountMapper pointAccountMapper;
    @Mock
    private ConsumptionOrderMapper consumptionOrderMapper;
    @Mock
    private AuditLogMapper auditLogMapper;

    private PrepareConsumptionServiceImpl service;

    @BeforeEach
    void setUp() {
        ConsumptionProperties properties = new ConsumptionProperties();
        properties.setPendingTtl(Duration.ofMinutes(5));
        Clock clock = Clock.fixed(Instant.parse("2026-09-05T12:00:00Z"), ZoneOffset.UTC);
        service = new PrepareConsumptionServiceImpl(
                customerUserMapper,
                staffStoreAuthorizationService,
                pointAccountMapper,
                consumptionOrderMapper,
                auditLogMapper,
                new ObjectMapper(),
                properties,
                clock
        );
    }

    @Test
    void shouldPreparePendingOrderWithoutDeductingPoints() {
        allowActiveCustomerAndStore();
        when(pointAccountMapper.selectByCustomerIdForUpdate(1L))
                .thenReturn(PointAccount.builder().id(9L).customerId(1L).availablePoints(150L).build());
        when(consumptionOrderMapper.insert(any(ConsumptionOrder.class))).thenAnswer(invocation -> {
            ConsumptionOrder order = invocation.getArgument(0);
            order.setId(20L);
            return 1;
        });
        when(auditLogMapper.insert(any(AuditLog.class))).thenReturn(1);

        PrepareConsumptionResult result = service.prepare(command());

        assertEquals(100L, result.consumePoints());
        assertEquals(10_000L, result.grossAmountCent());
        assertEquals(500, result.platformFeeRateBps());
        assertEquals(500L, result.platformFeeCent());
        assertEquals(9_500L, result.storePayableCent());
        assertEquals(ConsumptionVerificationMode.CUSTOMER_PIN, result.verificationMode());
        assertEquals(ConsumptionOrderStatus.PENDING_CONFIRM, result.orderStatus());
        assertEquals(NOW.plusMinutes(5), result.expiresTime());
        verify(consumptionOrderMapper).expirePendingByCustomerId(1L, NOW);

        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogMapper).insert(auditCaptor.capture());
        assertEquals("192.0.2.20", auditCaptor.getValue().getClientIp());
    }

    @Test
    void shouldReplaySameIdempotentPreparation() {
        allowActiveCustomerAndStore();
        ConsumptionOrder existing = existingOrder();
        when(consumptionOrderMapper.selectByIdempotencyKey("prepare-001")).thenReturn(existing);

        PrepareConsumptionResult result = service.prepare(command());

        assertEquals("CSM-EXISTING", result.orderNo());
        verify(pointAccountMapper, never()).selectByCustomerIdForUpdate(any());
        verify(consumptionOrderMapper, never()).insert(any(ConsumptionOrder.class));
    }

    @Test
    void shouldExpireStaleOrderBeforeIdempotentReplay() {
        allowActiveCustomerAndStore();
        ConsumptionOrder existing = existingOrder();
        existing.setExpiresTime(NOW.minusSeconds(1));
        when(consumptionOrderMapper.selectByIdempotencyKey("prepare-001")).thenReturn(existing);
        when(consumptionOrderMapper.expirePendingByCustomerId(1L, NOW)).thenReturn(1);

        PrepareConsumptionResult result = service.prepare(command());

        assertEquals(ConsumptionOrderStatus.EXPIRED, result.orderStatus());
        verify(pointAccountMapper, never()).selectByCustomerIdForUpdate(any());
    }

    @Test
    void shouldRejectDifferentRequestUsingSameIdempotencyKey() {
        allowActiveCustomerAndStore();
        ConsumptionOrder existing = existingOrder();
        existing.setConsumePoints(80L);
        when(consumptionOrderMapper.selectByIdempotencyKey("prepare-001")).thenReturn(existing);

        CustomException exception = assertThrows(CustomException.class, () -> service.prepare(command()));

        assertEquals(ExceptionEnum.PLATFORM_IDEMPOTENCY_CONFLICT.getCode(), exception.getCode());
    }

    @Test
    void shouldRejectWhenCustomerAlreadyHasActivePendingOrder() {
        allowActiveCustomerAndStore();
        when(pointAccountMapper.selectByCustomerIdForUpdate(1L))
                .thenReturn(PointAccount.builder().id(9L).availablePoints(150L).build());
        when(consumptionOrderMapper.selectActivePendingByCustomerId(1L))
                .thenReturn(existingOrder());

        CustomException exception = assertThrows(CustomException.class, () -> service.prepare(command()));

        assertEquals(ExceptionEnum.PLATFORM_CUSTOMER_HAS_PENDING_CONSUMPTION.getCode(), exception.getCode());
        verify(consumptionOrderMapper, never()).insert(any(ConsumptionOrder.class));
    }

    @Test
    void shouldRejectInsufficientBalanceAfterExpiringOldOrder() {
        allowActiveCustomerAndStore();
        when(pointAccountMapper.selectByCustomerIdForUpdate(1L))
                .thenReturn(PointAccount.builder().id(9L).availablePoints(99L).build());

        CustomException exception = assertThrows(CustomException.class, () -> service.prepare(command()));

        assertEquals(ExceptionEnum.PLATFORM_POINT_BALANCE_INSUFFICIENT.getCode(), exception.getCode());
        verify(consumptionOrderMapper).expirePendingByCustomerId(1L, NOW);
        verify(consumptionOrderMapper, never()).insert(any(ConsumptionOrder.class));
    }

    private void allowActiveCustomerAndStore() {
        when(customerUserMapper.selectById(1L))
                .thenReturn(CustomerUser.builder().id(1L).status(CustomerStatus.ACTIVE).build());
        when(staffStoreAuthorizationService.requireActiveStoreAccess(3L, 2L))
                .thenReturn(RoleCode.CLERK);
    }

    private PrepareConsumptionCommand command() {
        return new PrepareConsumptionCommand(
                1L,
                2L,
                3L,
                100L,
                "prepare-001",
                "客户现场消费",
                "192.0.2.20"
        );
    }

    private ConsumptionOrder existingOrder() {
        return ConsumptionOrder.builder()
                .orderNo("CSM-EXISTING")
                .customerId(1L)
                .storeId(2L)
                .consumePoints(100L)
                .grossAmountCent(10_000L)
                .platformFeeRateBps(500)
                .platformFeeCent(500L)
                .storePayableCent(9_500L)
                .verificationMode(ConsumptionVerificationMode.CUSTOMER_PIN)
                .expiresTime(NOW.plusMinutes(5))
                .orderStatus(ConsumptionOrderStatus.PENDING_CONFIRM)
                .operatorId(3L)
                .idempotencyKey("prepare-001")
                .remark("客户现场消费")
                .build();
    }
}
