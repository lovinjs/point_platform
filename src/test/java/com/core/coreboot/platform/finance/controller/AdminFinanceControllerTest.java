package com.core.coreboot.platform.finance.controller;

import com.core.coreboot.platform.auth.security.AdminUserPrincipal;
import com.core.coreboot.platform.common.enums.RoleCode;
import com.core.coreboot.platform.common.enums.SysUserStatus;
import com.core.coreboot.platform.finance.model.AdminFinancialReconciliationView;
import com.core.coreboot.platform.finance.model.AdminFinancialSummaryView;
import com.core.coreboot.platform.finance.service.AdminFinanceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.prepost.PreAuthorize;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminFinanceControllerTest {
    @Mock
    private AdminFinanceService financeService;
    @InjectMocks
    private AdminFinanceController controller;

    @Test
    void shouldRestrictFinanceCenterToSuperAdminAndDeriveOperatorIdentity() {
        PreAuthorize authorization = AdminFinanceController.class.getAnnotation(PreAuthorize.class);
        assertEquals("hasRole('SUPER_ADMIN')", authorization.value());

        LocalDate startDate = LocalDate.of(2026, 9, 1);
        LocalDate endDate = LocalDate.of(2026, 9, 10);
        AdminUserPrincipal principal = new AdminUserPrincipal(
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
        AdminFinancialSummaryView summary = new AdminFinancialSummaryView(
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
        );
        when(financeService.reconcile(5L, startDate, endDate, 2L))
                .thenReturn(new AdminFinancialReconciliationView(
                        startDate,
                        endDate,
                        LocalDateTime.of(2026, 9, 10, 10, 0),
                        summary,
                        List.of()
                ));

        controller.reconcile(startDate, endDate, 2L, principal);

        verify(financeService).reconcile(5L, startDate, endDate, 2L);
    }
}
