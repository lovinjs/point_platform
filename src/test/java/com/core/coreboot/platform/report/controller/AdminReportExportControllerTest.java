package com.core.coreboot.platform.report.controller;

import com.core.coreboot.platform.auth.security.AdminUserPrincipal;
import com.core.coreboot.platform.common.enums.RoleCode;
import com.core.coreboot.platform.common.enums.SysUserStatus;
import com.core.coreboot.platform.report.model.ExportedReport;
import com.core.coreboot.platform.report.service.AdminReportExportService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.prepost.PreAuthorize;

import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminReportExportControllerTest {
    @Mock
    private AdminReportExportService reportExportService;
    @InjectMocks
    private AdminReportExportController controller;

    @Test
    void shouldReturnUtf8AttachmentAndNoStoreHeaders() {
        LocalDate startDate = LocalDate.of(2026, 9, 1);
        LocalDate endDate = LocalDate.of(2026, 9, 10);
        byte[] content = {1, 2, 3};
        when(reportExportService.exportFinancialReconciliation(5L, startDate, endDate, null))
                .thenReturn(new ExportedReport(
                        "财务对账_20260901_20260910.xlsx",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                        content
                ));

        var response = controller.financialReconciliation(startDate, endDate, null, principal());

        assertArrayEquals(content, response.getBody());
        assertEquals("no-store", response.getHeaders().getCacheControl());
        assertTrue(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION).contains("UTF-8"));
        verify(reportExportService).exportFinancialReconciliation(5L, startDate, endDate, null);
    }

    @Test
    void shouldApplyManagerialExportRoleAndSuperAdminFinanceRole() throws Exception {
        PreAuthorize classAuthorization = AdminReportExportController.class.getAnnotation(PreAuthorize.class);
        PreAuthorize financeAuthorization = AdminReportExportController.class
                .getMethod(
                        "financialReconciliation",
                        LocalDate.class,
                        LocalDate.class,
                        Long.class,
                        AdminUserPrincipal.class
                )
                .getAnnotation(PreAuthorize.class);

        assertEquals("hasAnyRole('SUPER_ADMIN', 'STORE_MANAGER')", classAuthorization.value());
        assertEquals("hasRole('SUPER_ADMIN')", financeAuthorization.value());
    }

    private AdminUserPrincipal principal() {
        return new AdminUserPrincipal(
                5L,
                "platform.admin",
                "hash",
                "超级管理员",
                SysUserStatus.ACTIVE,
                null,
                0,
                Set.of(RoleCode.SUPER_ADMIN),
                Set.of()
        );
    }
}
