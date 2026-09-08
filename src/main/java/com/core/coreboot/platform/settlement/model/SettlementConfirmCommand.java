package com.core.coreboot.platform.settlement.model;

public record SettlementConfirmCommand(
        String settlementNo,
        Long operatorId,
        String remark,
        String clientIp
) {
}
