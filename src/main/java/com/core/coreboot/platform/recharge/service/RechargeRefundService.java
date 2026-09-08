package com.core.coreboot.platform.recharge.service;

import com.core.coreboot.platform.recharge.model.RechargeRefundCommand;
import com.core.coreboot.platform.recharge.model.RechargeRefundResult;

public interface RechargeRefundService {
    RechargeRefundResult refund(RechargeRefundCommand command);
}
