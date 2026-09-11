package com.core.coreboot.platform.merchant.model;

public record CustomerStoreView(
        Long storeId,
        String storeCode,
        String storeName,
        Long merchantId,
        String merchantName,
        String address,
        String contactPhone
) {
}
