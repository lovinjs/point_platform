package com.core.coreboot.platform.merchant.model;

import com.core.coreboot.platform.common.enums.MerchantStatus;

import java.time.LocalDateTime;

public record AdminMerchantManagementView(
        Long merchantId,
        String merchantCode,
        String legalName,
        String businessName,
        String unifiedSocialCreditCode,
        String contactName,
        String contactPhone,
        String agreementNo,
        MerchantStatus status,
        LocalDateTime createTime,
        LocalDateTime updateTime
) {
}
