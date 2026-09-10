package com.core.coreboot.platform.dashboard.model;

public record AdminDashboardBacklogView(
        long pendingConsumptionCount,
        long notIncludedConsumptionCount,
        long notIncludedPayableCent,
        long awaitingStoreConfirmationCount,
        long awaitingStoreConfirmationCent,
        long awaitingPlatformPaymentCount,
        long awaitingPlatformPaymentCent
) {
}
