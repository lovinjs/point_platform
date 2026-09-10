package com.core.coreboot.platform.dashboard.model;

import lombok.Data;

import java.time.LocalDate;

@Data
public class DashboardDailyAggregate {
    private LocalDate businessDate;
    private Long rechargeReceiptCent;
    private Long refundOutflowCent;
    private Long consumptionGrossCent;
    private Long platformFeeCent;
    private Long storePayableCent;
}
