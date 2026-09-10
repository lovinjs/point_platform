package com.core.coreboot.platform.report.service.impl;

import com.core.coreboot.platform.common.enums.ConsumptionOrderStatus;
import com.core.coreboot.platform.common.enums.RechargeOrderStatus;
import com.core.coreboot.platform.finance.model.AdminFinancialReconciliationView;
import com.core.coreboot.platform.finance.service.AdminFinanceService;
import com.core.coreboot.platform.order.model.AdminConsumptionOrderView;
import com.core.coreboot.platform.order.model.AdminOrderStoreOptionView;
import com.core.coreboot.platform.order.model.AdminRechargeOrderView;
import com.core.coreboot.platform.order.service.AdminOrderQueryService;
import com.core.coreboot.platform.report.model.ExportedReport;
import com.core.coreboot.platform.report.service.AdminReportExportService;
import com.core.coreboot.platform.report.support.AdminExcelReportWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminReportExportServiceImpl implements AdminReportExportService {
    private static final String XLSX_CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final DateTimeFormatter FILE_DATE = DateTimeFormatter.BASIC_ISO_DATE;

    private final AdminFinanceService financeService;
    private final AdminOrderQueryService orderQueryService;
    private final AdminExcelReportWriter reportWriter;

    @Override
    public ExportedReport exportFinancialReconciliation(
            Long operatorId,
            LocalDate startDate,
            LocalDate endDate,
            Long storeId
    ) {
        AdminFinancialReconciliationView view = financeService.reconcile(
                operatorId, startDate, endDate, storeId
        );
        String scopeLabel = storeId == null
                ? "全部门店"
                : view.stores().stream()
                .findFirst()
                .map(store -> store.storeName() + "（" + store.storeCode() + "）")
                .orElse("门店编号 " + storeId);
        return report(
                "财务对账_" + fileRange(startDate, endDate) + ".xlsx",
                reportWriter.writeFinancialReconciliation(view, scopeLabel)
        );
    }

    @Override
    public ExportedReport exportRechargeOrders(
            Long operatorId,
            LocalDate startDate,
            LocalDate endDate,
            Long storeId,
            RechargeOrderStatus status,
            String orderNo,
            String customerPhone
    ) {
        List<AdminRechargeOrderView> orders = orderQueryService.exportRechargeOrders(
                operatorId, startDate, endDate, storeId, status, orderNo, customerPhone
        );
        return report(
                "充值订单_" + fileRange(startDate, endDate) + ".xlsx",
                reportWriter.writeRechargeOrders(
                        startDate,
                        endDate,
                        resolveScopeLabel(operatorId, storeId),
                        filterLabel(rechargeStatusLabel(status), orderNo, customerPhone),
                        orders
                )
        );
    }

    @Override
    public ExportedReport exportConsumptionOrders(
            Long operatorId,
            LocalDate startDate,
            LocalDate endDate,
            Long storeId,
            ConsumptionOrderStatus status,
            String orderNo,
            String customerPhone
    ) {
        List<AdminConsumptionOrderView> orders = orderQueryService.exportConsumptionOrders(
                operatorId, startDate, endDate, storeId, status, orderNo, customerPhone
        );
        return report(
                "消费订单_" + fileRange(startDate, endDate) + ".xlsx",
                reportWriter.writeConsumptionOrders(
                        startDate,
                        endDate,
                        resolveScopeLabel(operatorId, storeId),
                        filterLabel(consumptionStatusLabel(status), orderNo, customerPhone),
                        orders
                )
        );
    }

    private ExportedReport report(String fileName, byte[] content) {
        return new ExportedReport(fileName, XLSX_CONTENT_TYPE, content);
    }

    private String resolveScopeLabel(Long operatorId, Long storeId) {
        if (storeId == null) {
            return "当前账号可访问的全部门店";
        }
        return orderQueryService.listStoreOptions(operatorId).stream()
                .filter(store -> store.storeId().equals(storeId))
                .findFirst()
                .map(this::storeLabel)
                .orElse("门店编号 " + storeId);
    }

    private String storeLabel(AdminOrderStoreOptionView store) {
        return store.storeName() + "（" + store.storeCode() + "）";
    }

    private String filterLabel(String status, String orderNo, String customerPhone) {
        StringBuilder label = new StringBuilder();
        appendFilter(label, "状态", status);
        appendFilter(label, "订单号包含", normalizeOptional(orderNo));
        appendFilter(label, "用户手机号", normalizeOptional(customerPhone));
        return label.isEmpty() ? "无额外筛选" : label.toString();
    }

    private void appendFilter(StringBuilder target, String name, String value) {
        if (value == null) {
            return;
        }
        if (!target.isEmpty()) {
            target.append("；");
        }
        target.append(name).append("：").append(value);
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String rechargeStatusLabel(RechargeOrderStatus status) {
        return status == null ? null : switch (status) {
            case CREATED -> "待支付";
            case COMPLETED -> "充值成功";
            case REFUNDED -> "已退款";
            case CANCELLED -> "已取消";
        };
    }

    private String consumptionStatusLabel(ConsumptionOrderStatus status) {
        return status == null ? null : switch (status) {
            case PENDING_CONFIRM -> "待用户确认";
            case COMPLETED -> "消费成功";
            case CANCELLED -> "已取消";
            case EXPIRED -> "已过期";
            case REVERSED -> "已冲正";
        };
    }

    private String fileRange(LocalDate startDate, LocalDate endDate) {
        return FILE_DATE.format(startDate) + "_" + FILE_DATE.format(endDate);
    }
}
