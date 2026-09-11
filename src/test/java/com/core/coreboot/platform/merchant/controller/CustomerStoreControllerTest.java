package com.core.coreboot.platform.merchant.controller;

import com.core.coreboot.platform.common.model.PageResult;
import com.core.coreboot.platform.merchant.model.CustomerStoreView;
import com.core.coreboot.platform.merchant.service.CustomerStoreQueryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerStoreControllerTest {
    @Mock
    private CustomerStoreQueryService storeQueryService;
    @InjectMocks
    private CustomerStoreController controller;

    @Test
    void shouldListStoresWithoutDependingOnCustomerIdentity() {
        PageResult<CustomerStoreView> page = new PageResult<>(
                1L, 20L, 0L, 0L, false, List.of()
        );
        when(storeQueryService.list(1, 20, "测试")).thenReturn(page);

        var response = controller.list(1, 20, "测试");

        assertEquals(page, response.getData());
        verify(storeQueryService).list(1, 20, "测试");
    }

    @Test
    void shouldReturnStoreDetail() {
        CustomerStoreView store = new CustomerStoreView(
                2L,
                "STORE-002",
                "测试门店",
                8L,
                "合作商户",
                "测试地址",
                "13800138000"
        );
        when(storeQueryService.getById(2L)).thenReturn(store);

        var response = controller.detail(2L);

        assertEquals(store, response.getData());
        verify(storeQueryService).getById(2L);
    }
}
