package com.core.coreboot.platform.customer.service.impl;

import com.core.coreboot.exception.CustomException;
import com.core.coreboot.exception.ExceptionEnum;
import com.core.coreboot.platform.common.enums.CustomerStatus;
import com.core.coreboot.platform.customer.entity.CustomerUser;
import com.core.coreboot.platform.customer.mapper.CustomerUserMapper;
import com.core.coreboot.platform.customer.model.AdminCustomerView;
import com.core.coreboot.platform.point.entity.PointAccount;
import com.core.coreboot.platform.point.mapper.PointAccountMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminCustomerQueryServiceImplTest {
    @Mock
    private CustomerUserMapper customerUserMapper;
    @Mock
    private PointAccountMapper pointAccountMapper;

    private AdminCustomerQueryServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AdminCustomerQueryServiceImpl(customerUserMapper, pointAccountMapper);
    }

    @Test
    void shouldReturnCustomerAndCurrentBalanceForExactPhone() {
        CustomerUser customer = CustomerUser.builder()
                .id(10L)
                .phone("13800138000")
                .nickname("测试客户")
                .status(CustomerStatus.ACTIVE)
                .build();
        when(customerUserMapper.selectByPhone("13800138000")).thenReturn(customer);
        when(pointAccountMapper.selectByCustomerId(10L))
                .thenReturn(PointAccount.builder().customerId(10L).availablePoints(88L).build());

        AdminCustomerView result = service.findByPhone(" 13800138000 ");

        assertEquals(10L, result.customerId());
        assertEquals(88L, result.availablePoints());
        assertEquals(CustomerStatus.ACTIVE, result.status());
    }

    @Test
    void shouldReturnZeroWhenCustomerHasNoPointAccountYet() {
        CustomerUser customer = CustomerUser.builder()
                .id(10L)
                .phone("13800138000")
                .status(CustomerStatus.ACTIVE)
                .build();
        when(customerUserMapper.selectByPhone("13800138000")).thenReturn(customer);

        AdminCustomerView result = service.findByPhone("13800138000");

        assertEquals(0L, result.availablePoints());
    }

    @Test
    void shouldRejectInvalidPhoneBeforeDatabaseQuery() {
        CustomException exception = assertThrows(
                CustomException.class,
                () -> service.findByPhone("1380abc")
        );

        assertEquals(ExceptionEnum.PLATFORM_INVALID_REQUEST.getCode(), exception.getCode());
        verify(customerUserMapper, never()).selectByPhone("1380abc");
    }

    @Test
    void shouldReturnNotFoundForUnknownPhone() {
        when(customerUserMapper.selectByPhone("13800138000")).thenReturn(null);

        CustomException exception = assertThrows(
                CustomException.class,
                () -> service.findByPhone("13800138000")
        );

        assertEquals(ExceptionEnum.PLATFORM_CUSTOMER_NOT_FOUND.getCode(), exception.getCode());
        verify(pointAccountMapper, never()).selectByCustomerId(10L);
    }
}
