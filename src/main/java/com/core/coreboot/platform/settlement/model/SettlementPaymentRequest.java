package com.core.coreboot.platform.settlement.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SettlementPaymentRequest(
        @NotBlank(message = "付款交易参考号不能为空")
        @Size(max = 128, message = "付款交易参考号不能超过128个字符")
        String paymentReference,

        @Size(max = 500, message = "付款备注不能超过500个字符")
        String remark
) {
    public SettlementPaymentCommand toCommand(
            String settlementNo,
            Long operatorId,
            String clientIp
    ) {
        return new SettlementPaymentCommand(
                settlementNo,
                operatorId,
                paymentReference,
                remark,
                clientIp
        );
    }
}
