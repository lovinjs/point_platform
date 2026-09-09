package com.core.coreboot.platform.merchant.model;

public record AdminMerchantCreateCommand(
        String merchantCode,
        String legalName,
        String businessName,
        String unifiedSocialCreditCode,
        String contactName,
        String contactPhone,
        String agreementNo,
        Long operatorId,
        String clientIp
) {
}
