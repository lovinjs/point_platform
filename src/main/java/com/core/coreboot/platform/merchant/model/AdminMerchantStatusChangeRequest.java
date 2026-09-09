package com.core.coreboot.platform.merchant.model;

import com.core.coreboot.platform.common.enums.MerchantStatus;
import jakarta.validation.constraints.NotNull;

public record AdminMerchantStatusChangeRequest(
        @NotNull(message = "目标状态不能为空")
        MerchantStatus status
) {
    public AdminMerchantStatusChangeCommand toCommand(Long merchantId, Long operatorId, String clientIp) {
        return new AdminMerchantStatusChangeCommand(merchantId, status, operatorId, clientIp);
    }
}
