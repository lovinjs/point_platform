package com.core.coreboot.platform.staff.service;

import com.core.coreboot.exception.CustomException;
import com.core.coreboot.exception.ExceptionEnum;
import com.core.coreboot.platform.common.enums.RoleCode;
import com.core.coreboot.platform.common.enums.MerchantStatus;
import com.core.coreboot.platform.common.enums.StoreStatus;
import com.core.coreboot.platform.common.enums.SysUserStatus;
import com.core.coreboot.platform.merchant.entity.Merchant;
import com.core.coreboot.platform.merchant.entity.Store;
import com.core.coreboot.platform.merchant.mapper.MerchantMapper;
import com.core.coreboot.platform.merchant.mapper.StoreMapper;
import com.core.coreboot.platform.staff.entity.SysUser;
import com.core.coreboot.platform.staff.mapper.StaffStoreAccessMapper;
import com.core.coreboot.platform.staff.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StaffStoreAuthorizationService {
    private final StoreMapper storeMapper;
    private final MerchantMapper merchantMapper;
    private final SysUserMapper sysUserMapper;
    private final StaffStoreAccessMapper staffStoreAccessMapper;

    public RoleCode requireActiveStoreAccess(Long operatorId, Long storeId) {
        if (operatorId == null || operatorId <= 0 || storeId == null || storeId <= 0) {
            throw new CustomException(ExceptionEnum.PLATFORM_INVALID_REQUEST);
        }

        Store store = storeMapper.selectById(storeId);
        if (store == null) {
            throw new CustomException(ExceptionEnum.PLATFORM_STORE_NOT_FOUND);
        }
        if (store.getStatus() != StoreStatus.ACTIVE) {
            throw new CustomException(ExceptionEnum.PLATFORM_STORE_UNAVAILABLE);
        }
        Merchant merchant = merchantMapper.selectById(store.getMerchantId());
        if (merchant == null || merchant.getStatus() != MerchantStatus.ACTIVE) {
            throw new CustomException(ExceptionEnum.PLATFORM_MERCHANT_UNAVAILABLE);
        }

        SysUser operator = sysUserMapper.selectById(operatorId);
        if (operator == null) {
            throw new CustomException(ExceptionEnum.PLATFORM_OPERATOR_NOT_FOUND);
        }
        if (operator.getStatus() != SysUserStatus.ACTIVE) {
            throw new CustomException(ExceptionEnum.PLATFORM_OPERATOR_DISABLED);
        }

        String roleCode = staffStoreAccessMapper.findEffectiveRoleCode(operatorId, storeId);
        if (roleCode == null) {
            throw new CustomException(ExceptionEnum.PLATFORM_STORE_ACCESS_DENIED);
        }
        try {
            return RoleCode.valueOf(roleCode);
        } catch (IllegalArgumentException ex) {
            throw new CustomException(ExceptionEnum.PLATFORM_STORE_ACCESS_DENIED);
        }
    }
}
