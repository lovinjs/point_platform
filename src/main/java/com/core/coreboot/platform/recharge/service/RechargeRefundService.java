package com.core.coreboot.platform.recharge.service;

import com.core.coreboot.platform.recharge.model.RechargeRefundCommand;
import com.core.coreboot.platform.recharge.model.RechargeRefundEligibility;
import com.core.coreboot.platform.recharge.model.RechargeRefundResult;

public interface RechargeRefundService {
    RechargeRefundEligibility getEligibility(Long operatorId, String orderNo);

    RechargeRefundResult refund(RechargeRefundCommand command);
}
