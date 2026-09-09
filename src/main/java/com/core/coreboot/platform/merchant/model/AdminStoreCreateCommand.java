package com.core.coreboot.platform.merchant.model;

public record AdminStoreCreateCommand(
        Long merchantId,
        String storeCode,
        String storeName,
        String address,
        String contactPhone,
        Long operatorId,
        String clientIp
) {
}
