package com.core.coreboot.platform.order.model;

import com.core.coreboot.platform.common.enums.ConsumptionOrderStatus;
import com.core.coreboot.platform.common.enums.ConsumptionVerificationMode;
import com.core.coreboot.platform.common.enums.SettlementStatus;

import java.time.LocalDateTime;

public record AdminConsumptionOrderView(
        String orderNo,
        Long customerId,
        String customerPhone,
        String customerNickname,
        Long storeId,
        String storeCode,
        String storeName,
        long consumePoints,
        long amountCent,
        int platformFeeRateBps,
        long platformFeeCent,
        long storePayableCent,
        ConsumptionVerificationMode verificationMode,
        ConsumptionOrderStatus orderStatus,
        SettlementStatus settlementStatus,
        Long operatorId,
        String operatorName,
        String remark,
        LocalDateTime expiresTime,
        LocalDateTime confirmedTime,
        LocalDateTime completedTime,
        LocalDateTime createTime
) {
}
