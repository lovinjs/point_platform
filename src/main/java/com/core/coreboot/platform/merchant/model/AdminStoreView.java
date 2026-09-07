package com.core.coreboot.platform.merchant.model;

public record AdminStoreView(
        Long storeId,
        String storeCode,
        String storeName,
        String address,
        String contactPhone
) {
}
