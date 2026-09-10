package com.core.coreboot.platform.report.service;

import com.core.coreboot.platform.common.enums.ConsumptionOrderStatus;
import com.core.coreboot.platform.common.enums.RechargeOrderStatus;
import com.core.coreboot.platform.report.model.ExportedReport;

import java.time.LocalDate;

public interface AdminReportExportService {
    ExportedReport exportFinancialReconciliation(
            Long operatorId,
            LocalDate startDate,
            LocalDate endDate,
            Long storeId
    );

    ExportedReport exportRechargeOrders(
            Long operatorId,
            LocalDate startDate,
            LocalDate endDate,
            Long storeId,
            RechargeOrderStatus status,
            String orderNo,
            String customerPhone
    );

    ExportedReport exportConsumptionOrders(
            Long operatorId,
            LocalDate startDate,
            LocalDate endDate,
            Long storeId,
            ConsumptionOrderStatus status,
            String orderNo,
            String customerPhone
    );
}
