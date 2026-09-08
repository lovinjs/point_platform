package com.core.coreboot.platform.consumption.service;

import com.core.coreboot.platform.consumption.model.ConsumptionConfirmationResult;
import com.core.coreboot.platform.consumption.model.CustomerPendingConsumptionView;

public interface CustomerConsumptionService {
    CustomerPendingConsumptionView getPending(Long customerId);

    ConsumptionConfirmationResult confirm(
            Long customerId,
            String orderNo,
            String consumePin,
            String clientIp
    );
}
