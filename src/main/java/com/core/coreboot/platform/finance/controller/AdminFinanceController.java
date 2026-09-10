package com.core.coreboot.platform.finance.controller;

import com.core.coreboot.common.ApiRestResponse;
import com.core.coreboot.platform.auth.security.AdminUserPrincipal;
import com.core.coreboot.platform.common.enums.AuditActorType;
import com.core.coreboot.platform.common.model.PageResult;
import com.core.coreboot.platform.finance.model.AdminAuditLogView;
import com.core.coreboot.platform.finance.model.AdminFinancialReconciliationView;
import com.core.coreboot.platform.finance.service.AdminFinanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "积分平台 - 财务对账与审计")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/finance")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AdminFinanceController {
    private final AdminFinanceService financeService;

    @Operation(summary = "按实际发生日期查询平台财务对账汇总")
    @GetMapping("/reconciliation")
    public ApiRestResponse<AdminFinancialReconciliationView> reconcile(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long storeId,
            @AuthenticationPrincipal AdminUserPrincipal principal
    ) {
        return ApiRestResponse.success(financeService.reconcile(
                principal.getUserId(), startDate, endDate, storeId
        ));
    }

    @Operation(summary = "分页查询关键业务操作审计日志")
    @GetMapping("/audit-logs")
    public ApiRestResponse<PageResult<AdminAuditLogView>> auditLogs(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long storeId,
            @RequestParam(required = false) AuditActorType actorType,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String keyword,
            @AuthenticationPrincipal AdminUserPrincipal principal
    ) {
        return ApiRestResponse.success(financeService.listAuditLogs(
                principal.getUserId(),
                pageNum,
                pageSize,
                startDate,
                endDate,
                storeId,
                actorType,
                action,
                keyword
        ));
    }

    @Operation(summary = "查询已有审计操作类型")
    @GetMapping("/audit-actions")
    public ApiRestResponse<List<String>> auditActions(
            @AuthenticationPrincipal AdminUserPrincipal principal
    ) {
        return ApiRestResponse.success(financeService.listAuditActions(principal.getUserId()));
    }
}
