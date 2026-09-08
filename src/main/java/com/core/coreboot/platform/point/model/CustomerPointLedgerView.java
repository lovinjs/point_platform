package com.core.coreboot.platform.point.model;

import com.core.coreboot.platform.common.enums.PointLedgerBusinessType;
import com.core.coreboot.platform.common.enums.PointLedgerType;

import java.time.LocalDateTime;

public record CustomerPointLedgerView(
        String ledgerNo,
        long deltaPoints,
        long balanceAfter,
        PointLedgerType ledgerType,
        PointLedgerBusinessType businessType,
        String businessNo,
        Long storeId,
        String storeName,
        String remark,
        LocalDateTime createTime
) {
}
