package com.core.coreboot.platform.dashboard.model;

import lombok.Data;

@Data
public class DashboardPeriodAggregate {
    private Long rechargeOrderCount;
    private Long rechargeReceiptCent;
    private Long rechargePoints;
    private Long refundOrderCount;
    private Long refundOutflowCent;
    private Long refundPoints;
    private Long consumptionOrderCount;
    private Long consumptionGrossCent;
    private Long consumptionPoints;
    private Long platformFeeCent;
    private Long storePayableCent;
}
