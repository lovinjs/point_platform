package com.core.coreboot.platform.merchant.model;

import com.core.coreboot.platform.common.enums.StoreStatus;

import java.time.LocalDateTime;

public record AdminStoreManagementView(
        Long storeId,
        Long merchantId,
        String merchantCode,
        String merchantName,
        String storeCode,
        String storeName,
        String address,
        String contactPhone,
        StoreStatus status,
        LocalDateTime createTime,
        LocalDateTime updateTime
) {
}
