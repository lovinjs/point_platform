package com.core.coreboot.platform.recharge.service;

import com.core.coreboot.platform.recharge.model.OfflineRechargeCommand;
import com.core.coreboot.platform.recharge.model.OfflineRechargeResult;

public interface OfflineRechargeService {
    OfflineRechargeResult recharge(OfflineRechargeCommand command);
}
