package com.core.coreboot.platform.customer.service;

import com.core.coreboot.platform.common.model.PageResult;
import com.core.coreboot.platform.consumption.model.CustomerConsumptionOrderView;
import com.core.coreboot.platform.point.model.CustomerPointBalanceView;
import com.core.coreboot.platform.point.model.CustomerPointLedgerView;
import com.core.coreboot.platform.recharge.model.CustomerRechargeOrderView;

public interface CustomerTransactionQueryService {
    CustomerPointBalanceView getPointBalance(Long customerId);

    PageResult<CustomerPointLedgerView> getPointLedger(Long customerId, int pageNum, int pageSize);

    PageResult<CustomerRechargeOrderView> getRechargeOrders(Long customerId, int pageNum, int pageSize);

    PageResult<CustomerConsumptionOrderView> getConsumptionOrders(Long customerId, int pageNum, int pageSize);
}
