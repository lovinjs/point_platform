package com.core.coreboot.platform.settlement.model;

public record SettlementGenerateCommand(
        String periodCode,
        Long operatorId,
        String clientIp
) {
}
