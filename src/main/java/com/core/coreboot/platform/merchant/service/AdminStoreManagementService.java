package com.core.coreboot.platform.merchant.service;

import com.core.coreboot.platform.common.enums.StoreStatus;
import com.core.coreboot.platform.common.model.PageResult;
import com.core.coreboot.platform.merchant.model.AdminMerchantOptionView;
import com.core.coreboot.platform.merchant.model.AdminStoreCreateCommand;
import com.core.coreboot.platform.merchant.model.AdminStoreManagementView;
import com.core.coreboot.platform.merchant.model.AdminStoreStatusChangeCommand;
import com.core.coreboot.platform.merchant.model.AdminStoreUpdateCommand;

import java.util.List;

public interface AdminStoreManagementService {
    PageResult<AdminStoreManagementView> list(
            Long operatorId,
            int pageNum,
            int pageSize,
            Long merchantId,
            StoreStatus status,
            String keyword
    );

    List<AdminMerchantOptionView> listActiveMerchantOptions(Long operatorId);

    AdminStoreManagementView create(AdminStoreCreateCommand command);

    AdminStoreManagementView update(AdminStoreUpdateCommand command);

    AdminStoreManagementView changeStatus(AdminStoreStatusChangeCommand command);
}
