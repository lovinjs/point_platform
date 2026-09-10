package com.core.coreboot.platform.dashboard.controller;

import com.core.coreboot.platform.auth.security.AdminUserPrincipal;
import com.core.coreboot.platform.common.enums.RoleCode;
import com.core.coreboot.platform.common.enums.SysUserStatus;
import com.core.coreboot.platform.dashboard.model.AdminDashboardBacklogView;
import com.core.coreboot.platform.dashboard.model.AdminDashboardOverviewView;
import com.core.coreboot.platform.dashboard.model.AdminDashboardPeriodMetricsView;
import com.core.coreboot.platform.dashboard.service.AdminDashboardService;
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
class AdminDashboardControllerTest {
    @Mock
    private AdminDashboardService dashboardService;
    @InjectMocks
    private AdminDashboardController controller;

    @Test
    void shouldRestrictOverviewAndDeriveOperatorIdentity() {
        PreAuthorize authorization = AdminDashboardController.class.getAnnotation(PreAuthorize.class);
        assertEquals(
                "hasAnyRole('SUPER_ADMIN', 'STORE_MANAGER')",
                authorization.value()
        );
        AdminUserPrincipal principal = new AdminUserPrincipal(
                5L,
                "manager",
                "hash",
                "测试店长",
                SysUserStatus.ACTIVE,
                null,
                0,
                Set.of(RoleCode.STORE_MANAGER),
                Set.of(2L)
        );
        AdminDashboardPeriodMetricsView period = new AdminDashboardPeriodMetricsView(
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
        );
        AdminDashboardBacklogView backlog = new AdminDashboardBacklogView(0, 0, 0, 0, 0, 0, 0);
        when(dashboardService.overview(5L, 2L, 7)).thenReturn(new AdminDashboardOverviewView(
                LocalDateTime.of(2026, 9, 10, 10, 0),
                LocalDate.of(2026, 9, 10),
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 4),
                7,
                false,
                2L,
                "STORE-2",
                "测试门店2",
                period,
                period,
                backlog,
                List.of()
        ));

        controller.overview(2L, 7, principal);

        verify(dashboardService).overview(5L, 2L, 7);
    }
}
