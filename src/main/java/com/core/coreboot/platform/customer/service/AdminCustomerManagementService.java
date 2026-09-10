package com.core.coreboot.platform.customer.service;

import com.core.coreboot.platform.common.enums.CustomerStatus;
import com.core.coreboot.platform.common.model.PageResult;
import com.core.coreboot.platform.consumption.model.CustomerConsumptionOrderView;
import com.core.coreboot.platform.customer.model.AdminCustomerManagementView;
import com.core.coreboot.platform.customer.model.AdminCustomerStatusChangeCommand;
import com.core.coreboot.platform.point.model.CustomerPointLedgerView;
import com.core.coreboot.platform.recharge.model.CustomerRechargeOrderView;

public interface AdminCustomerManagementService {
    PageResult<AdminCustomerManagementView> list(
            Long operatorId,
            int pageNum,
            int pageSize,
            CustomerStatus status,
            String keyword
    );

    AdminCustomerManagementView get(Long operatorId, Long customerId);

    PageResult<CustomerPointLedgerView> getPointLedger(
            Long operatorId,
            Long customerId,
            int pageNum,
            int pageSize
    );

    PageResult<CustomerRechargeOrderView> getRechargeOrders(
            Long operatorId,
            Long customerId,
            int pageNum,
            int pageSize
    );

    PageResult<CustomerConsumptionOrderView> getConsumptionOrders(
            Long operatorId,
            Long customerId,
            int pageNum,
            int pageSize
    );

    AdminCustomerManagementView changeStatus(AdminCustomerStatusChangeCommand command);
}
