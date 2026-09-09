package com.core.coreboot.platform.staff.service;

import com.core.coreboot.platform.common.enums.RoleCode;
import com.core.coreboot.platform.common.enums.SysUserStatus;
import com.core.coreboot.platform.common.model.PageResult;
import com.core.coreboot.platform.staff.model.AdminStaffCreateCommand;
import com.core.coreboot.platform.staff.model.AdminStaffManagementView;
import com.core.coreboot.platform.staff.model.AdminStaffPasswordResetCommand;
import com.core.coreboot.platform.staff.model.AdminStaffStatusChangeCommand;
import com.core.coreboot.platform.staff.model.AdminStaffStoreView;
import com.core.coreboot.platform.staff.model.AdminStaffUnlockCommand;
import com.core.coreboot.platform.staff.model.AdminStaffUpdateCommand;

import java.util.List;

public interface AdminStaffManagementService {
    PageResult<AdminStaffManagementView> list(
            Long operatorId,
            int pageNum,
            int pageSize,
            RoleCode roleCode,
            SysUserStatus status,
            Long storeId,
            String keyword
    );

    List<AdminStaffStoreView> listAssignableStores(Long operatorId);

    AdminStaffManagementView create(AdminStaffCreateCommand command);

    AdminStaffManagementView update(AdminStaffUpdateCommand command);

    AdminStaffManagementView changeStatus(AdminStaffStatusChangeCommand command);

    void resetPassword(AdminStaffPasswordResetCommand command);

    AdminStaffManagementView unlock(AdminStaffUnlockCommand command);
}
