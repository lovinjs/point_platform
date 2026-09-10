package com.core.coreboot.platform.order.model;

import com.core.coreboot.platform.common.enums.StoreStatus;

public record AdminOrderStoreOptionView(
        Long storeId,
        String storeCode,
        String storeName,
        StoreStatus storeStatus
) {
}
