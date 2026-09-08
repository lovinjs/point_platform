package com.core.coreboot.platform.settlement.model;

import com.core.coreboot.platform.common.enums.SettlementPeriodStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record SettlementGenerationResult(
        String periodCode,
        LocalDate startDate,
        LocalDate endDate,
        SettlementPeriodStatus periodStatus,
        LocalDateTime generatedTime,
        int settlementCount,
        List<StoreSettlementSummaryView> settlements
) {
    public SettlementGenerationResult {
        settlements = settlements == null ? List.of() : List.copyOf(settlements);
    }
}
