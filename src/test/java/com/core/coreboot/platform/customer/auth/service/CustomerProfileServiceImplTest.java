package com.core.coreboot.platform.customer.auth.service;

import com.core.coreboot.exception.CustomException;
import com.core.coreboot.exception.ExceptionEnum;
import com.core.coreboot.platform.common.enums.CustomerStatus;
import com.core.coreboot.platform.customer.auth.model.CustomerAccountView;
import com.core.coreboot.platform.customer.entity.CustomerSecurity;
import com.core.coreboot.platform.customer.entity.CustomerUser;
import com.core.coreboot.platform.customer.mapper.CustomerSecurityMapper;
import com.core.coreboot.platform.customer.mapper.CustomerUserMapper;
import com.core.coreboot.platform.point.entity.PointAccount;
import com.core.coreboot.platform.point.mapper.PointAccountMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerProfileServiceImplTest {
    @Mock
    private CustomerUserMapper customerUserMapper;
    @Mock
    private CustomerSecurityMapper customerSecurityMapper;
    @Mock
    private PointAccountMapper pointAccountMapper;

    private CustomerProfileServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CustomerProfileServiceImpl(
                customerUserMapper,
                customerSecurityMapper,
                pointAccountMapper
        );
    }

    @Test
    void shouldReturnMaskedProfilePinStatusAndBalance() {
        when(customerUserMapper.selectById(9L)).thenReturn(CustomerUser.builder()
                .id(9L)
                .phone("13800138000")
                .nickname("测试客户")
                .avatarUrl("https://example.invalid/avatar.png")
                .status(CustomerStatus.ACTIVE)
                .build());
        when(customerSecurityMapper.selectById(9L)).thenReturn(CustomerSecurity.builder()
                .customerId(9L)
                .consumePinHash("bcrypt-hash")
                .build());
        when(pointAccountMapper.selectByCustomerId(9L)).thenReturn(PointAccount.builder()
                .customerId(9L)
                .availablePoints(128L)
                .build());

        CustomerAccountView result = service.getProfile(9L);

        assertEquals("138****8000", result.maskedPhone());
        assertEquals(128L, result.availablePoints());
        assertTrue(result.phoneBound());
        assertTrue(result.consumePinConfigured());
    }

    @Test
    void shouldRejectDisabledCustomer() {
        when(customerUserMapper.selectById(9L)).thenReturn(CustomerUser.builder()
                .id(9L)
                .status(CustomerStatus.DISABLED)
                .build());

        CustomException exception = assertThrows(CustomException.class, () -> service.getProfile(9L));

        assertEquals(ExceptionEnum.PLATFORM_CUSTOMER_DISABLED.getCode(), exception.getCode());
    }
}
