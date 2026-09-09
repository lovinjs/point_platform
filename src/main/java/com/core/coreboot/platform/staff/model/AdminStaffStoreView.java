package com.core.coreboot.platform.staff.model;

import com.core.coreboot.platform.common.enums.MerchantStatus;
import com.core.coreboot.platform.common.enums.StoreStatus;

public record AdminStaffStoreView(
        Long storeId,
        String storeCode,
        String storeName,
        StoreStatus storeStatus,
        Long merchantId,
        String merchantName,
        MerchantStatus merchantStatus
) {
}
