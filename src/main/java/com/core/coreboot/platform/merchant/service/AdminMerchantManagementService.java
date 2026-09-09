package com.core.coreboot.platform.merchant.service;

import com.core.coreboot.platform.common.enums.MerchantStatus;
import com.core.coreboot.platform.common.model.PageResult;
import com.core.coreboot.platform.merchant.model.AdminMerchantCreateCommand;
import com.core.coreboot.platform.merchant.model.AdminMerchantManagementView;
import com.core.coreboot.platform.merchant.model.AdminMerchantStatusChangeCommand;
import com.core.coreboot.platform.merchant.model.AdminMerchantUpdateCommand;

public interface AdminMerchantManagementService {
    PageResult<AdminMerchantManagementView> list(
            Long operatorId,
            int pageNum,
            int pageSize,
            MerchantStatus status,
            String keyword
    );

    AdminMerchantManagementView create(AdminMerchantCreateCommand command);

    AdminMerchantManagementView update(AdminMerchantUpdateCommand command);

    AdminMerchantManagementView changeStatus(AdminMerchantStatusChangeCommand command);
}
