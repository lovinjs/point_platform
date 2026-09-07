package com.core.coreboot.platform.merchant.service;

import com.core.coreboot.platform.merchant.model.AdminStoreView;

import java.util.List;

public interface AdminStoreQueryService {
    List<AdminStoreView> listAccessibleActiveStores(Long operatorId);
}
