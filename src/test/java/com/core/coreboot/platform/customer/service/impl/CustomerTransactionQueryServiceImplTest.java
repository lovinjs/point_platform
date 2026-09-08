package com.core.coreboot.platform.customer.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.core.coreboot.exception.CustomException;
import com.core.coreboot.exception.ExceptionEnum;
import com.core.coreboot.platform.common.enums.ConsumptionOrderStatus;
import com.core.coreboot.platform.common.enums.PaymentMethod;
import com.core.coreboot.platform.common.enums.PointLedgerBusinessType;
import com.core.coreboot.platform.common.enums.PointLedgerType;
import com.core.coreboot.platform.common.enums.RechargeChannel;
import com.core.coreboot.platform.common.enums.RechargeOrderStatus;
import com.core.coreboot.platform.consumption.entity.ConsumptionOrder;
import com.core.coreboot.platform.consumption.mapper.ConsumptionOrderMapper;
import com.core.coreboot.platform.merchant.entity.Store;
import com.core.coreboot.platform.merchant.mapper.StoreMapper;
import com.core.coreboot.platform.point.entity.PointAccount;
import com.core.coreboot.platform.point.entity.PointLedger;
import com.core.coreboot.platform.point.mapper.PointAccountMapper;
import com.core.coreboot.platform.point.mapper.PointLedgerMapper;
import com.core.coreboot.platform.recharge.entity.RechargeOrder;
import com.core.coreboot.platform.recharge.mapper.RechargeOrderMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
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
class CustomerTransactionQueryServiceImplTest {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 8, 15, 0);

    @Mock
    private PointAccountMapper pointAccountMapper;
    @Mock
    private PointLedgerMapper pointLedgerMapper;
    @Mock
    private RechargeOrderMapper rechargeOrderMapper;
    @Mock
    private ConsumptionOrderMapper consumptionOrderMapper;
    @Mock
    private StoreMapper storeMapper;

    private CustomerTransactionQueryServiceImpl service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-09-08T15:00:00Z"), ZoneOffset.UTC);
        service = new CustomerTransactionQueryServiceImpl(
                pointAccountMapper,
                pointLedgerMapper,
                rechargeOrderMapper,
                consumptionOrderMapper,
                storeMapper,
                clock
        );
    }

    @Test
    void shouldReturnZeroWhenPointAccountDoesNotExist() {
        when(pointAccountMapper.selectByCustomerId(7L)).thenReturn(null);

        var result = service.getPointBalance(7L);

        assertEquals(0L, result.availablePoints());
        assertEquals(null, result.updatedTime());
    }

    @Test
    void shouldReturnCustomerLedgerWithStoreNameAndPagination() {
        PointLedger ledger = PointLedger.builder()
                .id(31L)
                .ledgerNo("LDG31")
                .customerId(7L)
                .deltaPoints(-30L)
                .balanceAfter(970L)
                .ledgerType(PointLedgerType.CONSUME)
                .businessType(PointLedgerBusinessType.CONSUMPTION_ORDER)
                .businessNo("CSM31")
                .storeId(2L)
                .remark("现场消费")
                .createTime(NOW)
                .build();
        Page<PointLedger> page = pageOf(2, 1, 3, List.of(ledger));
        when(pointLedgerMapper.selectPage(
                ArgumentMatchers.<Page<PointLedger>>any(),
                ArgumentMatchers.<Wrapper<PointLedger>>any()
        )).thenReturn(page);
        when(storeMapper.selectList(ArgumentMatchers.<Wrapper<Store>>any()))
                .thenReturn(List.of(Store.builder().id(2L).storeName("测试门店").build()));

        var result = service.getPointLedger(7L, 2, 1);

        assertEquals(2L, result.pageNum());
        assertEquals(3L, result.total());
        assertEquals(3L, result.totalPages());
        assertTrue(result.hasNext());
        assertEquals("测试门店", result.items().getFirst().storeName());
        assertEquals(-30L, result.items().getFirst().deltaPoints());
    }

    @Test
    void shouldReturnRechargeOrdersWithoutExposingPaymentReference() {
        RechargeOrder order = RechargeOrder.builder()
                .id(11L)
                .orderNo("RCH11")
                .customerId(7L)
                .rechargeStoreId(2L)
                .rechargePoints(1_000L)
                .amountCent(100_000L)
                .channel(RechargeChannel.OFFLINE)
                .paymentMethod(PaymentMethod.OTHER)
                .paymentReference("INTERNAL-REFERENCE")
                .orderStatus(RechargeOrderStatus.COMPLETED)
                .completedTime(NOW)
                .createTime(NOW)
                .build();
        when(rechargeOrderMapper.selectPage(
                ArgumentMatchers.<Page<RechargeOrder>>any(),
                ArgumentMatchers.<Wrapper<RechargeOrder>>any()
        )).thenReturn(pageOf(1, 20, 1, List.of(order)));
        when(storeMapper.selectList(ArgumentMatchers.<Wrapper<Store>>any()))
                .thenReturn(List.of(Store.builder().id(2L).storeName("测试门店").build()));

        var result = service.getRechargeOrders(7L, 1, 20);

        assertFalse(result.hasNext());
        assertEquals("RCH11", result.items().getFirst().orderNo());
        assertEquals(1_000L, result.items().getFirst().rechargePoints());
        assertEquals("测试门店", result.items().getFirst().storeName());
    }

    @Test
    void shouldExpireStalePendingOrdersBeforeReturningConsumptionHistory() {
        ConsumptionOrder order = ConsumptionOrder.builder()
                .id(21L)
                .orderNo("CSM21")
                .customerId(7L)
                .storeId(2L)
                .consumePoints(30L)
                .grossAmountCent(3_000L)
                .orderStatus(ConsumptionOrderStatus.COMPLETED)
                .completedTime(NOW)
                .createTime(NOW.minusMinutes(2))
                .build();
        when(consumptionOrderMapper.selectPage(
                ArgumentMatchers.<Page<ConsumptionOrder>>any(),
                ArgumentMatchers.<Wrapper<ConsumptionOrder>>any()
        )).thenReturn(pageOf(1, 20, 1, List.of(order)));
        when(storeMapper.selectList(ArgumentMatchers.<Wrapper<Store>>any()))
                .thenReturn(List.of(Store.builder().id(2L).storeName("测试门店").build()));

        var result = service.getConsumptionOrders(7L, 1, 20);

        verify(consumptionOrderMapper).expirePendingByCustomerId(7L, NOW);
        assertEquals(ConsumptionOrderStatus.COMPLETED, result.items().getFirst().orderStatus());
        assertEquals(3_000L, result.items().getFirst().amountCent());
    }

    @Test
    void shouldRejectOversizedPageBeforeQueryingDatabase() {
        CustomException exception = assertThrows(
                CustomException.class,
                () -> service.getPointLedger(7L, 1, 51)
        );

        assertEquals(ExceptionEnum.PLATFORM_INVALID_REQUEST.getCode(), exception.getCode());
        verify(pointLedgerMapper, never()).selectPage(any(), any());
    }

    private <T> Page<T> pageOf(long current, long size, long total, List<T> records) {
        Page<T> page = new Page<>(current, size);
        page.setTotal(total);
        page.setRecords(records);
        return page;
    }
}
