package com.core.coreboot.platform.merchant.model;

public record AdminMerchantOptionView(
        Long merchantId,
        String merchantCode,
        String businessName
) {
}
