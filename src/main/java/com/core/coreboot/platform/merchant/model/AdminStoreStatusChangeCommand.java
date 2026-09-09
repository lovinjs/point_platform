package com.core.coreboot.platform.merchant.model;

import com.core.coreboot.platform.common.enums.StoreStatus;

public record AdminStoreStatusChangeCommand(
        Long storeId,
        StoreStatus status,
        Long operatorId,
        String clientIp
) {
}
