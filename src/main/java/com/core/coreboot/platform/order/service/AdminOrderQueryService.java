package com.core.coreboot.platform.order.service;

import com.core.coreboot.platform.common.enums.ConsumptionOrderStatus;
import com.core.coreboot.platform.common.enums.RechargeOrderStatus;
import com.core.coreboot.platform.common.model.PageResult;
import com.core.coreboot.platform.order.model.AdminConsumptionOrderView;
import com.core.coreboot.platform.order.model.AdminOrderStoreOptionView;
import com.core.coreboot.platform.order.model.AdminRechargeOrderView;

import java.time.LocalDate;
import java.util.List;

public interface AdminOrderQueryService {
    List<AdminOrderStoreOptionView> listStoreOptions(Long operatorId);

    PageResult<AdminRechargeOrderView> listRechargeOrders(
            Long operatorId,
            int pageNum,
            int pageSize,
            Long storeId,
            RechargeOrderStatus status,
            String orderNo,
            String customerPhone
    );

    PageResult<AdminConsumptionOrderView> listConsumptionOrders(
            Long operatorId,
            int pageNum,
            int pageSize,
            Long storeId,
            ConsumptionOrderStatus status,
            String orderNo,
            String customerPhone
    );

    List<AdminRechargeOrderView> exportRechargeOrders(
            Long operatorId,
            LocalDate startDate,
            LocalDate endDate,
            Long storeId,
            RechargeOrderStatus status,
            String orderNo,
            String customerPhone
    );

    List<AdminConsumptionOrderView> exportConsumptionOrders(
            Long operatorId,
            LocalDate startDate,
            LocalDate endDate,
            Long storeId,
            ConsumptionOrderStatus status,
            String orderNo,
            String customerPhone
    );
}
