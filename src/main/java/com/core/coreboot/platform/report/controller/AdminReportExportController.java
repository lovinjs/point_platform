package com.core.coreboot.platform.report.controller;

import com.core.coreboot.platform.auth.security.AdminUserPrincipal;
import com.core.coreboot.platform.common.enums.ConsumptionOrderStatus;
import com.core.coreboot.platform.common.enums.RechargeOrderStatus;
import com.core.coreboot.platform.report.model.ExportedReport;
import com.core.coreboot.platform.report.service.AdminReportExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

@Tag(name = "积分平台 - Excel报表导出")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/reports")
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'STORE_MANAGER')")
public class AdminReportExportController {
    private final AdminReportExportService reportExportService;

    @Operation(summary = "导出财务对账报表")
    @GetMapping("/financial-reconciliation")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<byte[]> financialReconciliation(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long storeId,
            @AuthenticationPrincipal AdminUserPrincipal principal
    ) {
        return download(reportExportService.exportFinancialReconciliation(
                principal.getUserId(), startDate, endDate, storeId
        ));
    }

    @Operation(summary = "按订单创建日期导出充值订单")
    @GetMapping("/recharge-orders")
    public ResponseEntity<byte[]> rechargeOrders(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long storeId,
            @RequestParam(required = false) RechargeOrderStatus status,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) String customerPhone,
            @AuthenticationPrincipal AdminUserPrincipal principal
    ) {
        return download(reportExportService.exportRechargeOrders(
                principal.getUserId(), startDate, endDate, storeId, status, orderNo, customerPhone
        ));
    }

    @Operation(summary = "按订单创建日期导出消费订单")
    @GetMapping("/consumption-orders")
    public ResponseEntity<byte[]> consumptionOrders(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long storeId,
            @RequestParam(required = false) ConsumptionOrderStatus status,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) String customerPhone,
            @AuthenticationPrincipal AdminUserPrincipal principal
    ) {
        return download(reportExportService.exportConsumptionOrders(
                principal.getUserId(), startDate, endDate, storeId, status, orderNo, customerPhone
        ));
    }

    private ResponseEntity<byte[]> download(ExportedReport report) {
        byte[] content = report.content();
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(report.fileName(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(report.contentType()))
                .contentLength(content.length)
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(content);
    }
}
