package com.core.coreboot.platform.merchant.model;

import com.core.coreboot.platform.common.enums.StoreStatus;
import jakarta.validation.constraints.NotNull;

public record AdminStoreStatusChangeRequest(
        @NotNull(message = "目标状态不能为空")
        StoreStatus status
) {
    public AdminStoreStatusChangeCommand toCommand(Long storeId, Long operatorId, String clientIp) {
        return new AdminStoreStatusChangeCommand(storeId, status, operatorId, clientIp);
    }
}
