package com.core.coreboot.platform.dashboard.service;

import com.core.coreboot.platform.dashboard.model.AdminDashboardOverviewView;

public interface AdminDashboardService {
    AdminDashboardOverviewView overview(Long operatorId, Long storeId, int trendDays);
}
