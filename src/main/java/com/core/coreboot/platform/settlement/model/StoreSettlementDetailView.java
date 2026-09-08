package com.core.coreboot.platform.settlement.model;

import java.util.List;

public record StoreSettlementDetailView(
        StoreSettlementSummaryView settlement,
        List<StoreSettlementItemView> items
) {
    public StoreSettlementDetailView {
        items = items == null ? List.of() : List.copyOf(items);
    }
}
