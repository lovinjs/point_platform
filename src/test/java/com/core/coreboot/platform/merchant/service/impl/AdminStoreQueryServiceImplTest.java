package com.core.coreboot.platform.merchant.service.impl;

import com.core.coreboot.platform.merchant.entity.Store;
import com.core.coreboot.platform.merchant.mapper.StoreMapper;
import com.core.coreboot.platform.merchant.model.AdminStoreView;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminStoreQueryServiceImplTest {
    @Mock
    private StoreMapper storeMapper;
    @InjectMocks
    private AdminStoreQueryServiceImpl service;

    @Test
    void shouldMapAccessibleActiveStores() {
        when(storeMapper.selectAccessibleActiveStores(7L)).thenReturn(List.of(
                Store.builder()
                        .id(20L)
                        .storeCode("STORE-001")
                        .storeName("合作门店")
                        .address("测试地址")
                        .contactPhone("13800138000")
                        .build()
        ));

        List<AdminStoreView> result = service.listAccessibleActiveStores(7L);

        assertEquals(1, result.size());
        assertEquals(20L, result.getFirst().storeId());
        assertEquals("合作门店", result.getFirst().storeName());
    }
}
