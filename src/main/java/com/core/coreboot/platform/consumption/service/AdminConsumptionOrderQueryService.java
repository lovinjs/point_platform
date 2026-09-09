package com.core.coreboot.platform.consumption.service;

import com.core.coreboot.platform.consumption.model.AdminConsumptionOrderStatusView;

public interface AdminConsumptionOrderQueryService {
    AdminConsumptionOrderStatusView getStatus(Long operatorId, String orderNo);
}
