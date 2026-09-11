package com.core.coreboot.platform.merchant.service;

import com.core.coreboot.platform.common.model.PageResult;
import com.core.coreboot.platform.merchant.model.CustomerStoreView;

public interface CustomerStoreQueryService {
    PageResult<CustomerStoreView> list(int pageNum, int pageSize, String keyword);

    CustomerStoreView getById(Long storeId);
}
