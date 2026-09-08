package com.core.coreboot.platform.settlement.controller;

import com.core.coreboot.common.ApiRestResponse;
import com.core.coreboot.platform.auth.security.AdminUserPrincipal;
import com.core.coreboot.platform.common.enums.StoreSettlementStatus;
import com.core.coreboot.platform.common.model.PageResult;
import com.core.coreboot.platform.settlement.model.SettlementConfirmCommand;
import com.core.coreboot.platform.settlement.model.SettlementConfirmRequest;
import com.core.coreboot.platform.settlement.model.SettlementGenerateCommand;
import com.core.coreboot.platform.settlement.model.SettlementGenerationResult;
import com.core.coreboot.platform.settlement.model.SettlementPaymentRequest;
import com.core.coreboot.platform.settlement.model.StoreSettlementDetailView;
import com.core.coreboot.platform.settlement.model.StoreSettlementSummaryView;
import com.core.coreboot.platform.settlement.service.SettlementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "积分平台 - 门店结算")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'STORE_MANAGER')")
public class AdminSettlementController {
    private final SettlementService settlementService;

    @Operation(summary = "按已结束的自然月生成门店结算单")
    @PostMapping("/settlement-periods/{periodCode}/generate")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiRestResponse<SettlementGenerationResult> generate(
            @Parameter(description = "自然月，格式为 YYYY-MM", example = "2026-08")
            @PathVariable String periodCode,
            @AuthenticationPrincipal AdminUserPrincipal principal,
            HttpServletRequest servletRequest
    ) {
        return ApiRestResponse.success(settlementService.generate(new SettlementGenerateCommand(
                periodCode,
                principal.getUserId(),
                servletRequest.getRemoteAddr()
        )));
    }

    @Operation(summary = "分页查询门店结算单")
    @GetMapping("/settlements")
    public ApiRestResponse<PageResult<StoreSettlementSummaryView>> list(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String periodCode,
            @RequestParam(required = false) StoreSettlementStatus status,
            @AuthenticationPrincipal AdminUserPrincipal principal
    ) {
        return ApiRestResponse.success(settlementService.list(
                principal.getUserId(),
                pageNum,
                pageSize,
                periodCode,
                status
        ));
    }

    @Operation(summary = "查询门店结算单及消费明细")
    @GetMapping("/settlements/{settlementNo}")
    public ApiRestResponse<StoreSettlementDetailView> detail(
            @PathVariable String settlementNo,
            @AuthenticationPrincipal AdminUserPrincipal principal
    ) {
        return ApiRestResponse.success(settlementService.detail(
                principal.getUserId(),
                settlementNo
        ));
    }

    @Operation(summary = "店长确认本门店结算单")
    @PostMapping("/settlements/{settlementNo}/confirm")
    @PreAuthorize("hasRole('STORE_MANAGER')")
    public ApiRestResponse<StoreSettlementDetailView> confirm(
            @PathVariable String settlementNo,
            @Valid @RequestBody(required = false) SettlementConfirmRequest request,
            @AuthenticationPrincipal AdminUserPrincipal principal,
            HttpServletRequest servletRequest
    ) {
        SettlementConfirmCommand command = request == null
                ? new SettlementConfirmCommand(
                        settlementNo,
                        principal.getUserId(),
                        null,
                        servletRequest.getRemoteAddr()
                )
                : request.toCommand(
                        settlementNo,
                        principal.getUserId(),
                        servletRequest.getRemoteAddr()
                );
        return ApiRestResponse.success(settlementService.confirm(command));
    }

    @Operation(summary = "超级管理员登记结算付款")
    @PostMapping("/settlements/{settlementNo}/mark-paid")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiRestResponse<StoreSettlementDetailView> markPaid(
            @PathVariable String settlementNo,
            @Valid @RequestBody SettlementPaymentRequest request,
            @AuthenticationPrincipal AdminUserPrincipal principal,
            HttpServletRequest servletRequest
    ) {
        return ApiRestResponse.success(settlementService.markPaid(request.toCommand(
                settlementNo,
                principal.getUserId(),
                servletRequest.getRemoteAddr()
        )));
    }
}
