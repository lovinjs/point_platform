package com.core.coreboot.platform.settlement.model;

import jakarta.validation.constraints.Size;

public record SettlementConfirmRequest(
        @Size(max = 500, message = "确认备注不能超过500个字符")
        String remark
) {
    public SettlementConfirmCommand toCommand(
            String settlementNo,
            Long operatorId,
            String clientIp
    ) {
        return new SettlementConfirmCommand(settlementNo, operatorId, remark, clientIp);
    }
}
