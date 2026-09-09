package com.core.coreboot.platform.merchant.model;

public record AdminStoreUpdateCommand(
        Long storeId,
        String storeName,
        String address,
        String contactPhone,
        Long operatorId,
        String clientIp
) {
}
