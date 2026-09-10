package com.core.coreboot.platform.finance.service;

import com.core.coreboot.platform.common.enums.AuditActorType;
import com.core.coreboot.platform.common.model.PageResult;
import com.core.coreboot.platform.finance.model.AdminAuditLogView;
import com.core.coreboot.platform.finance.model.AdminFinancialReconciliationView;

import java.time.LocalDate;
import java.util.List;

public interface AdminFinanceService {
    AdminFinancialReconciliationView reconcile(
            Long operatorId,
            LocalDate startDate,
            LocalDate endDate,
            Long storeId
    );

    PageResult<AdminAuditLogView> listAuditLogs(
            Long operatorId,
            int pageNum,
            int pageSize,
            LocalDate startDate,
            LocalDate endDate,
            Long storeId,
            AuditActorType actorType,
            String action,
            String keyword
    );

    List<String> listAuditActions(Long operatorId);
}
