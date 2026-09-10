package com.core.coreboot.platform.dashboard.model;

import lombok.Data;

@Data
public class DashboardBacklogAggregate {
    private Long pendingConsumptionCount;
    private Long notIncludedConsumptionCount;
    private Long notIncludedPayableCent;
    private Long awaitingStoreConfirmationCount;
    private Long awaitingStoreConfirmationCent;
    private Long awaitingPlatformPaymentCount;
    private Long awaitingPlatformPaymentCent;
}
