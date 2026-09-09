package com.core.coreboot.platform.merchant.model;

import com.core.coreboot.platform.common.enums.MerchantStatus;

public record AdminMerchantStatusChangeCommand(
        Long merchantId,
        MerchantStatus status,
        Long operatorId,
        String clientIp
) {
}
