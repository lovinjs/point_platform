package com.core.coreboot.platform.finance.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record AdminFinancialReconciliationView(
        LocalDate startDate,
        LocalDate endDate,
        LocalDateTime generatedTime,
        AdminFinancialSummaryView summary,
        List<AdminFinancialStoreView> stores
) {
    public AdminFinancialReconciliationView {
        stores = stores == null ? List.of() : List.copyOf(stores);
    }
}
