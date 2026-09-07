package com.core.coreboot.platform.consumption.service;

import com.core.coreboot.platform.consumption.model.PrepareConsumptionCommand;
import com.core.coreboot.platform.consumption.model.PrepareConsumptionResult;

public interface PrepareConsumptionService {
    PrepareConsumptionResult prepare(PrepareConsumptionCommand command);
}
