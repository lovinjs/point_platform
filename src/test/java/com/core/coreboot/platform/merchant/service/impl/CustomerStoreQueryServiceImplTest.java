package com.core.coreboot.platform.merchant.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.core.coreboot.exception.CustomException;
import com.core.coreboot.exception.ExceptionEnum;
import com.core.coreboot.platform.common.enums.MerchantStatus;
import com.core.coreboot.platform.common.enums.StoreStatus;
import com.core.coreboot.platform.merchant.entity.Merchant;
import com.core.coreboot.platform.merchant.entity.Store;
import com.core.coreboot.platform.merchant.mapper.MerchantMapper;
import com.core.coreboot.platform.merchant.mapper.StoreMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerStoreQueryServiceImplTest {
    @Mock
    private StoreMapper storeMapper;
    @Mock
    private MerchantMapper merchantMapper;

    private CustomerStoreQueryServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CustomerStoreQueryServiceImpl(storeMapper, merchantMapper);
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldListOnlyPublicStoreFieldsWithMerchantName() {
        Page<Store> page = new Page<>(1, 20, 1);
        page.setRecords(List.of(store(StoreStatus.ACTIVE)));
        when(storeMapper.selectPage(any(Page.class), any(Wrapper.class))).thenReturn(page);
        when(merchantMapper.selectByIds(List.of(8L)))
                .thenReturn(List.of(merchant(MerchantStatus.ACTIVE)));

        var result = service.list(1, 20, " 测试 ");

        assertEquals(1L, result.total());
        assertEquals(1, result.items().size());
        assertEquals("测试门店", result.items().getFirst().storeName());
        assertEquals("合作商户", result.items().getFirst().merchantName());
        assertEquals("13800138000", result.items().getFirst().contactPhone());
    }

    @Test
    void shouldReturnActiveStoreDetail() {
        when(storeMapper.selectById(2L)).thenReturn(store(StoreStatus.ACTIVE));
        when(merchantMapper.selectById(8L)).thenReturn(merchant(MerchantStatus.ACTIVE));

        var result = service.getById(2L);

        assertEquals(2L, result.storeId());
        assertEquals("STORE-002", result.storeCode());
        assertEquals("测试市测试区合作路 8 号", result.address());
    }

    @Test
    void shouldHideStoreWhenMerchantIsNotActive() {
        when(storeMapper.selectById(2L)).thenReturn(store(StoreStatus.ACTIVE));
        when(merchantMapper.selectById(8L)).thenReturn(merchant(MerchantStatus.SUSPENDED));

        CustomException exception = assertThrows(CustomException.class, () -> service.getById(2L));

        assertEquals(ExceptionEnum.PLATFORM_STORE_NOT_FOUND.getCode(), exception.getCode());
    }

    @Test
    void shouldRejectInvalidPageBeforeQueryingDatabase() {
        CustomException exception = assertThrows(
                CustomException.class,
                () -> service.list(0, 20, null)
        );

        assertEquals(ExceptionEnum.PLATFORM_INVALID_REQUEST.getCode(), exception.getCode());
        verify(storeMapper, never()).selectPage(any(), any());
    }

    private Store store(StoreStatus status) {
        return Store.builder()
                .id(2L)
                .merchantId(8L)
                .storeCode("STORE-002")
                .storeName("测试门店")
                .address("测试市测试区合作路 8 号")
                .contactPhone("13800138000")
                .status(status)
                .build();
    }

    private Merchant merchant(MerchantStatus status) {
        return Merchant.builder()
                .id(8L)
                .businessName("合作商户")
                .status(status)
                .build();
    }
}
