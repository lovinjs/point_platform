package com.core.coreboot.platform.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.core.coreboot.exception.CustomException;
import com.core.coreboot.exception.ExceptionEnum;
import com.core.coreboot.platform.common.enums.ConsumptionOrderStatus;
import com.core.coreboot.platform.common.enums.ConsumptionVerificationMode;
import com.core.coreboot.platform.common.enums.FundReceiver;
import com.core.coreboot.platform.common.enums.PaymentMethod;
import com.core.coreboot.platform.common.enums.RechargeChannel;
import com.core.coreboot.platform.common.enums.RechargeOrderStatus;
import com.core.coreboot.platform.common.enums.RechargeRefundStatus;
import com.core.coreboot.platform.common.enums.RefundMethod;
import com.core.coreboot.platform.common.enums.SettlementStatus;
import com.core.coreboot.platform.common.enums.StoreStatus;
import com.core.coreboot.platform.common.enums.SysUserStatus;
import com.core.coreboot.platform.consumption.entity.ConsumptionOrder;
import com.core.coreboot.platform.consumption.mapper.ConsumptionOrderMapper;
import com.core.coreboot.platform.customer.entity.CustomerUser;
import com.core.coreboot.platform.customer.mapper.CustomerUserMapper;
import com.core.coreboot.platform.merchant.entity.Store;
import com.core.coreboot.platform.merchant.mapper.StoreMapper;
import com.core.coreboot.platform.recharge.entity.RechargeOrder;
import com.core.coreboot.platform.recharge.entity.RechargeRefund;
import com.core.coreboot.platform.recharge.mapper.RechargeOrderMapper;
import com.core.coreboot.platform.recharge.mapper.RechargeRefundMapper;
import com.core.coreboot.platform.staff.entity.SysUser;
import com.core.coreboot.platform.staff.mapper.StaffAuthorityMapper;
import com.core.coreboot.platform.staff.mapper.SysUserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminOrderQueryServiceImplTest {
    private static final Long OPERATOR_ID = 9L;
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 10, 10, 0);

    @Mock
    private RechargeOrderMapper rechargeOrderMapper;
    @Mock
    private RechargeRefundMapper rechargeRefundMapper;
    @Mock
    private ConsumptionOrderMapper consumptionOrderMapper;
    @Mock
    private CustomerUserMapper customerUserMapper;
    @Mock
    private StoreMapper storeMapper;
    @Mock
    private SysUserMapper sysUserMapper;
    @Mock
    private StaffAuthorityMapper staffAuthorityMapper;

    private AdminOrderQueryServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AdminOrderQueryServiceImpl(
                rechargeOrderMapper,
                rechargeRefundMapper,
                consumptionOrderMapper,
                customerUserMapper,
                storeMapper,
                sysUserMapper,
                staffAuthorityMapper,
                Clock.fixed(Instant.parse("2026-09-10T02:00:00Z"), ZoneId.of("Asia/Shanghai"))
        );
    }

    @Test
    void shouldReturnStoreScopedRechargeOrdersWithRefundDetail() {
        allowClerkAtStore(2L);
        RechargeOrder order = RechargeOrder.builder()
                .id(11L)
                .orderNo("RCH11")
                .customerId(7L)
                .rechargeStoreId(2L)
                .rechargePoints(1_000L)
                .amountCent(100_000L)
                .channel(RechargeChannel.OFFLINE)
                .paymentMethod(PaymentMethod.OTHER)
                .fundReceiver(FundReceiver.PLATFORM)
                .paymentReference("PAY-11")
                .orderStatus(RechargeOrderStatus.REFUNDED)
                .operatorId(OPERATOR_ID)
                .createTime(NOW.minusDays(1))
                .build();
        RechargeRefund refund = RechargeRefund.builder()
                .refundNo("RF11")
                .rechargeOrderId(11L)
                .refundMethod(RefundMethod.BANK_TRANSFER)
                .refundReference("BANK-RF-11")
                .refundStatus(RechargeRefundStatus.COMPLETED)
                .reason("客户申请全额退款")
                .completedTime(NOW)
                .build();
        when(rechargeOrderMapper.selectPage(
                ArgumentMatchers.<Page<RechargeOrder>>any(),
                ArgumentMatchers.<Wrapper<RechargeOrder>>any()
        )).thenReturn(pageOf(1, 20, 1, List.of(order)));
        when(customerUserMapper.selectByIds(List.of(7L))).thenReturn(List.of(
                CustomerUser.builder().id(7L).phone("13800138000").nickname("测试客户").build()
        ));
        when(storeMapper.selectByIds(List.of(2L))).thenReturn(List.of(store(2L, StoreStatus.ACTIVE)));
        when(sysUserMapper.selectByIds(List.of(OPERATOR_ID))).thenReturn(List.of(
                SysUser.builder().id(OPERATOR_ID).username("clerk").realName("店员甲").build()
        ));
        when(rechargeRefundMapper.selectList(ArgumentMatchers.<Wrapper<RechargeRefund>>any()))
                .thenReturn(List.of(refund));

        var result = service.listRechargeOrders(OPERATOR_ID, 1, 20, null, null, null, null);

        assertEquals(1, result.total());
        assertEquals("测试客户", result.items().getFirst().customerNickname());
        assertEquals("测试门店2", result.items().getFirst().storeName());
        assertEquals("店员甲", result.items().getFirst().operatorName());
        assertEquals("RF11", result.items().getFirst().refundNo());
        assertEquals(RechargeRefundStatus.COMPLETED, result.items().getFirst().refundStatus());
    }

    @Test
    void shouldRejectStoreOutsideStaffScopeBeforeOrderQuery() {
        allowClerkAtStore(2L);

        CustomException exception = assertThrows(
                CustomException.class,
                () -> service.listRechargeOrders(OPERATOR_ID, 1, 20, 3L, null, null, null)
        );

        assertEquals(ExceptionEnum.PLATFORM_STORE_ACCESS_DENIED.getCode(), exception.getCode());
        verify(rechargeOrderMapper, never()).selectPage(ArgumentMatchers.any(), ArgumentMatchers.any());
    }

    @Test
    void shouldDisplayStalePendingConsumptionAsExpired() {
        allowClerkAtStore(2L);
        ConsumptionOrder order = ConsumptionOrder.builder()
                .id(21L)
                .orderNo("CSM21")
                .customerId(7L)
                .storeId(2L)
                .consumePoints(30L)
                .grossAmountCent(3_000L)
                .platformFeeRateBps(500)
                .platformFeeCent(150L)
                .storePayableCent(2_850L)
                .verificationMode(ConsumptionVerificationMode.CUSTOMER_PIN)
                .expiresTime(NOW.minusSeconds(1))
                .orderStatus(ConsumptionOrderStatus.PENDING_CONFIRM)
                .settlementStatus(SettlementStatus.NOT_INCLUDED)
                .operatorId(OPERATOR_ID)
                .createTime(NOW.minusMinutes(5))
                .build();
        when(consumptionOrderMapper.selectPage(
                ArgumentMatchers.<Page<ConsumptionOrder>>any(),
                ArgumentMatchers.<Wrapper<ConsumptionOrder>>any()
        )).thenReturn(pageOf(1, 20, 1, List.of(order)));
        when(customerUserMapper.selectByIds(List.of(7L))).thenReturn(List.of(
                CustomerUser.builder().id(7L).phone("13800138000").build()
        ));
        when(storeMapper.selectByIds(List.of(2L))).thenReturn(List.of(store(2L, StoreStatus.ACTIVE)));
        when(sysUserMapper.selectByIds(List.of(OPERATOR_ID))).thenReturn(List.of(
                SysUser.builder().id(OPERATOR_ID).username("clerk").build()
        ));

        var result = service.listConsumptionOrders(OPERATOR_ID, 1, 20, null, null, null, null);

        assertEquals(ConsumptionOrderStatus.EXPIRED, result.items().getFirst().orderStatus());
        assertEquals(2_850L, result.items().getFirst().storePayableCent());
    }

    @Test
    void shouldReturnEmptyPageForUnknownExactPhoneWithoutOrderQuery() {
        allowClerkAtStore(2L);
        when(customerUserMapper.selectByPhone("13800138000")).thenReturn(null);

        var result = service.listConsumptionOrders(
                OPERATOR_ID, 2, 20, null, null, null, "13800138000"
        );

        assertTrue(result.items().isEmpty());
        assertEquals(0, result.total());
        assertEquals(2, result.pageNum());
        verify(consumptionOrderMapper, never()).selectPage(ArgumentMatchers.any(), ArgumentMatchers.any());
    }

    @Test
    void shouldLetSuperAdminSeeHistoricalStoresOfEveryStatus() {
        allowSuperAdmin();
        when(storeMapper.selectList(ArgumentMatchers.<Wrapper<Store>>any())).thenReturn(List.of(
                store(2L, StoreStatus.ACTIVE),
                store(3L, StoreStatus.SUSPENDED),
                store(4L, StoreStatus.CLOSED)
        ));

        var result = service.listStoreOptions(OPERATOR_ID);

        assertEquals(3, result.size());
        assertTrue(result.stream().anyMatch(store -> store.storeStatus() == StoreStatus.CLOSED));
        verify(staffAuthorityMapper, never()).selectStoreIds(OPERATOR_ID);
    }

    @Test
    void shouldRejectClerkOrderExport() {
        when(sysUserMapper.selectById(OPERATOR_ID)).thenReturn(activeOperator());
        when(staffAuthorityMapper.selectRoleCodes(OPERATOR_ID)).thenReturn(List.of("CLERK"));

        CustomException exception = assertThrows(
                CustomException.class,
                () -> service.exportRechargeOrders(
                        OPERATOR_ID,
                        LocalDate.of(2026, 9, 1),
                        LocalDate.of(2026, 9, 10),
                        null,
                        null,
                        null,
                        null
                )
        );

        assertEquals(ExceptionEnum.PLATFORM_ADMIN_ACCESS_DENIED.getCode(), exception.getCode());
        verify(rechargeOrderMapper, never()).selectList(ArgumentMatchers.any());
    }

    @Test
    void shouldExportManagerOrdersWithinAssignedStoreScope() {
        allowManagerAtStore(2L);
        RechargeOrder order = RechargeOrder.builder()
                .id(31L)
                .orderNo("RCH31")
                .customerId(7L)
                .rechargeStoreId(2L)
                .rechargePoints(100L)
                .amountCent(10_000L)
                .channel(RechargeChannel.OFFLINE)
                .paymentMethod(PaymentMethod.BANK_TRANSFER)
                .fundReceiver(FundReceiver.PLATFORM)
                .paymentReference("BANK-31")
                .orderStatus(RechargeOrderStatus.COMPLETED)
                .operatorId(OPERATOR_ID)
                .createTime(NOW.minusDays(1))
                .build();
        when(rechargeOrderMapper.selectList(ArgumentMatchers.<Wrapper<RechargeOrder>>any()))
                .thenReturn(List.of(order));
        when(customerUserMapper.selectByIds(List.of(7L))).thenReturn(List.of(
                CustomerUser.builder().id(7L).phone("13800138000").build()
        ));
        when(storeMapper.selectByIds(List.of(2L))).thenReturn(List.of(store(2L, StoreStatus.ACTIVE)));
        when(sysUserMapper.selectByIds(List.of(OPERATOR_ID))).thenReturn(List.of(
                SysUser.builder().id(OPERATOR_ID).username("manager").build()
        ));
        when(rechargeRefundMapper.selectList(ArgumentMatchers.<Wrapper<RechargeRefund>>any()))
                .thenReturn(List.of());

        var result = service.exportRechargeOrders(
                OPERATOR_ID,
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 10),
                null,
                RechargeOrderStatus.COMPLETED,
                null,
                null
        );

        assertEquals(1, result.size());
        assertEquals("RCH31", result.getFirst().orderNo());
        assertEquals("测试门店2", result.getFirst().storeName());
    }

    @Test
    void shouldRejectOversizedOrderExportRangeBeforeQuery() {
        allowSuperAdmin();

        CustomException exception = assertThrows(
                CustomException.class,
                () -> service.exportConsumptionOrders(
                        OPERATOR_ID,
                        LocalDate.of(2026, 6, 9),
                        LocalDate.of(2026, 9, 10),
                        null,
                        null,
                        null,
                        null
                )
        );

        assertEquals(ExceptionEnum.PLATFORM_INVALID_REQUEST.getCode(), exception.getCode());
        verify(consumptionOrderMapper, never()).selectList(ArgumentMatchers.any());
    }

    @Test
    void shouldRejectOrderExportAboveRowLimitBeforeLoadingRelatedData() {
        allowSuperAdmin();
        RechargeOrder order = RechargeOrder.builder().id(41L).build();
        when(rechargeOrderMapper.selectList(ArgumentMatchers.<Wrapper<RechargeOrder>>any()))
                .thenReturn(Collections.nCopies(10_001, order));

        CustomException exception = assertThrows(
                CustomException.class,
                () -> service.exportRechargeOrders(
                        OPERATOR_ID,
                        LocalDate.of(2026, 9, 1),
                        LocalDate.of(2026, 9, 10),
                        null,
                        null,
                        null,
                        null
                )
        );

        assertEquals(ExceptionEnum.PLATFORM_REPORT_ROW_LIMIT_EXCEEDED.getCode(), exception.getCode());
        verify(customerUserMapper, never()).selectByIds(ArgumentMatchers.anyCollection());
    }

    private void allowClerkAtStore(Long storeId) {
        when(sysUserMapper.selectById(OPERATOR_ID)).thenReturn(activeOperator());
        when(staffAuthorityMapper.selectRoleCodes(OPERATOR_ID)).thenReturn(List.of("CLERK"));
        when(staffAuthorityMapper.selectStoreIds(OPERATOR_ID)).thenReturn(List.of(storeId));
    }

    private void allowSuperAdmin() {
        when(sysUserMapper.selectById(OPERATOR_ID)).thenReturn(activeOperator());
        when(staffAuthorityMapper.selectRoleCodes(OPERATOR_ID)).thenReturn(List.of("SUPER_ADMIN"));
    }

    private void allowManagerAtStore(Long storeId) {
        when(sysUserMapper.selectById(OPERATOR_ID)).thenReturn(activeOperator());
        when(staffAuthorityMapper.selectRoleCodes(OPERATOR_ID)).thenReturn(List.of("STORE_MANAGER"));
        when(staffAuthorityMapper.selectStoreIds(OPERATOR_ID)).thenReturn(List.of(storeId));
    }

    private SysUser activeOperator() {
        return SysUser.builder().id(OPERATOR_ID).status(SysUserStatus.ACTIVE).build();
    }

    private Store store(Long id, StoreStatus status) {
        return Store.builder()
                .id(id)
                .storeCode("STORE-" + id)
                .storeName("测试门店" + id)
                .status(status)
                .build();
    }

    private <T> Page<T> pageOf(long current, long size, long total, List<T> records) {
        Page<T> page = new Page<>(current, size);
        page.setTotal(total);
        page.setRecords(records);
        return page;
    }
}
