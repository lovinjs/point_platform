package com.core.coreboot.platform.settlement.model;

public record SettlementPaymentCommand(
        String settlementNo,
        Long operatorId,
        String paymentReference,
        String remark,
        String clientIp
) {
}
