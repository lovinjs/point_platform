package com.core.coreboot.platform.consumption.model;

import com.core.coreboot.platform.common.enums.SettlementStatus;

import java.time.LocalDateTime;

public record ConsumptionReversalResult(
        String ledgerNo,
        String consumptionOrderNo,
        Long customerId,
        Long storeId,
        long reversedPoints,
        long availablePoints,
        SettlementStatus settlementStatus,
        LocalDateTime reversedTime
) {
}
