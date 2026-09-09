package com.core.coreboot.platform.staff.service;

import com.core.coreboot.exception.CustomException;
import com.core.coreboot.exception.ExceptionEnum;
import com.core.coreboot.platform.common.enums.RoleCode;
import com.core.coreboot.platform.common.enums.MerchantStatus;
import com.core.coreboot.platform.common.enums.StoreStatus;
import com.core.coreboot.platform.common.enums.SysUserStatus;
import com.core.coreboot.platform.merchant.entity.Store;
import com.core.coreboot.platform.merchant.entity.Merchant;
import com.core.coreboot.platform.merchant.mapper.MerchantMapper;
import com.core.coreboot.platform.merchant.mapper.StoreMapper;
import com.core.coreboot.platform.staff.entity.SysUser;
import com.core.coreboot.platform.staff.mapper.StaffStoreAccessMapper;
import com.core.coreboot.platform.staff.mapper.SysUserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StaffStoreAuthorizationServiceTest {
    @Mock
    private StoreMapper storeMapper;
    @Mock
    private MerchantMapper merchantMapper;
    @Mock
    private SysUserMapper sysUserMapper;
    @Mock
    private StaffStoreAccessMapper staffStoreAccessMapper;
    @InjectMocks
    private StaffStoreAuthorizationService service;

    @Test
    void shouldReturnEffectiveRoleForActiveOperatorAndStore() {
        when(storeMapper.selectById(2L))
                .thenReturn(Store.builder().id(2L).merchantId(4L).status(StoreStatus.ACTIVE).build());
        when(merchantMapper.selectById(4L))
                .thenReturn(Merchant.builder().id(4L).status(MerchantStatus.ACTIVE).build());
        when(sysUserMapper.selectById(3L))
                .thenReturn(SysUser.builder().id(3L).status(SysUserStatus.ACTIVE).build());
        when(staffStoreAccessMapper.findEffectiveRoleCode(3L, 2L))
                .thenReturn("STORE_MANAGER");

        RoleCode result = service.requireActiveStoreAccess(3L, 2L);

        assertEquals(RoleCode.STORE_MANAGER, result);
    }

    @Test
    void shouldRejectOperatorWithoutStoreScope() {
        when(storeMapper.selectById(2L))
                .thenReturn(Store.builder().id(2L).merchantId(4L).status(StoreStatus.ACTIVE).build());
        when(merchantMapper.selectById(4L))
                .thenReturn(Merchant.builder().id(4L).status(MerchantStatus.ACTIVE).build());
        when(sysUserMapper.selectById(3L))
                .thenReturn(SysUser.builder().id(3L).status(SysUserStatus.ACTIVE).build());
        when(staffStoreAccessMapper.findEffectiveRoleCode(3L, 2L)).thenReturn(null);

        CustomException exception = assertThrows(
                CustomException.class,
                () -> service.requireActiveStoreAccess(3L, 2L)
        );

        assertEquals(ExceptionEnum.PLATFORM_STORE_ACCESS_DENIED.getCode(), exception.getCode());
    }

    @Test
    void shouldRejectActiveStoreWhenMerchantIsSuspended() {
        when(storeMapper.selectById(2L))
                .thenReturn(Store.builder().id(2L).merchantId(4L).status(StoreStatus.ACTIVE).build());
        when(merchantMapper.selectById(4L))
                .thenReturn(Merchant.builder().id(4L).status(MerchantStatus.SUSPENDED).build());

        CustomException exception = assertThrows(
                CustomException.class,
                () -> service.requireActiveStoreAccess(3L, 2L)
        );

        assertEquals(ExceptionEnum.PLATFORM_MERCHANT_UNAVAILABLE.getCode(), exception.getCode());
    }
}
