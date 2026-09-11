package com.core.coreboot.platform.consumption.service;

import com.core.coreboot.platform.consumption.model.ConsumptionReversalCommand;
import com.core.coreboot.platform.consumption.model.ConsumptionReversalResult;

public interface ConsumptionReversalService {
    ConsumptionReversalResult reverse(ConsumptionReversalCommand command);
}
