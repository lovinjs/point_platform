package com.core.coreboot.platform.finance.model;

import lombok.Data;

@Data
public class FinancialStoreAggregate {
    private Long storeId;
    private Long rechargeReceiptCent;
    private Long rechargePoints;
    private Long refundOutflowCent;
    private Long refundPoints;
    private Long consumptionGrossCent;
    private Long consumptionPoints;
    private Long platformFeeCent;
    private Long storePayableCent;
    private Long notIncludedPayableCent;
    private Long includedPayableCent;
    private Long settledPayableCent;
    private Long settlementPaidCent;
}
