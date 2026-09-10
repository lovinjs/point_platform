package com.core.coreboot.platform.report.service.impl;

import com.core.coreboot.platform.common.enums.RechargeOrderStatus;
import com.core.coreboot.platform.common.enums.StoreStatus;
import com.core.coreboot.platform.finance.model.AdminFinancialReconciliationView;
import com.core.coreboot.platform.finance.model.AdminFinancialSummaryView;
import com.core.coreboot.platform.finance.service.AdminFinanceService;
import com.core.coreboot.platform.order.model.AdminOrderStoreOptionView;
import com.core.coreboot.platform.order.service.AdminOrderQueryService;
import com.core.coreboot.platform.report.support.AdminExcelReportWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminReportExportServiceImplTest {
    private static final LocalDate START_DATE = LocalDate.of(2026, 9, 1);
    private static final LocalDate END_DATE = LocalDate.of(2026, 9, 10);

    @Mock
    private AdminFinanceService financeService;
    @Mock
    private AdminOrderQueryService orderQueryService;
    @Mock
    private AdminExcelReportWriter reportWriter;
    @InjectMocks
    private AdminReportExportServiceImpl service;

    @Test
    void shouldReuseFinanceReconciliationResultAndCreateStableFileName() {
        AdminFinancialReconciliationView view = new AdminFinancialReconciliationView(
                START_DATE,
                END_DATE,
                LocalDateTime.of(2026, 9, 10, 12, 0),
                new AdminFinancialSummaryView(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
                List.of()
        );
        byte[] bytes = {1, 2};
        when(financeService.reconcile(9L, START_DATE, END_DATE, null)).thenReturn(view);
        when(reportWriter.writeFinancialReconciliation(view, "全部门店")).thenReturn(bytes);

        var result = service.exportFinancialReconciliation(9L, START_DATE, END_DATE, null);

        assertEquals("财务对账_20260901_20260910.xlsx", result.fileName());
        assertArrayEquals(bytes, result.content());
    }

    @Test
    void shouldPassReadableScopeAndFiltersToRechargeReport() {
        when(orderQueryService.exportRechargeOrders(
                9L, START_DATE, END_DATE, 2L, RechargeOrderStatus.COMPLETED, " RCH ", " 13800138000 "
        )).thenReturn(List.of());
        when(orderQueryService.listStoreOptions(9L)).thenReturn(List.of(
                new AdminOrderStoreOptionView(2L, "STORE-2", "测试门店", StoreStatus.ACTIVE)
        ));
        when(reportWriter.writeRechargeOrders(
                START_DATE,
                END_DATE,
                "测试门店（STORE-2）",
                "状态：充值成功；订单号包含：RCH；用户手机号：13800138000",
                List.of()
        )).thenReturn(new byte[]{3, 4});

        var result = service.exportRechargeOrders(
                9L,
                START_DATE,
                END_DATE,
                2L,
                RechargeOrderStatus.COMPLETED,
                " RCH ",
                " 13800138000 "
        );

        assertEquals("充值订单_20260901_20260910.xlsx", result.fileName());
        verify(reportWriter).writeRechargeOrders(
                START_DATE,
                END_DATE,
                "测试门店（STORE-2）",
                "状态：充值成功；订单号包含：RCH；用户手机号：13800138000",
                List.of()
        );
    }
}
