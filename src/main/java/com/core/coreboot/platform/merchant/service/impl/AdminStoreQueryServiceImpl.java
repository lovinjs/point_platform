package com.core.coreboot.platform.merchant.service.impl;

import com.core.coreboot.exception.CustomException;
import com.core.coreboot.exception.ExceptionEnum;
import com.core.coreboot.platform.merchant.mapper.StoreMapper;
import com.core.coreboot.platform.merchant.model.AdminStoreView;
import com.core.coreboot.platform.merchant.service.AdminStoreQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminStoreQueryServiceImpl implements AdminStoreQueryService {
    private final StoreMapper storeMapper;

    @Override
    public List<AdminStoreView> listAccessibleActiveStores(Long operatorId) {
        if (operatorId == null || operatorId <= 0) {
            throw new CustomException(ExceptionEnum.PLATFORM_ADMIN_TOKEN_INVALID);
        }
        return storeMapper.selectAccessibleActiveStores(operatorId).stream()
                .map(store -> new AdminStoreView(
                        store.getId(),
                        store.getStoreCode(),
                        store.getStoreName(),
                        store.getAddress(),
                        store.getContactPhone()
                ))
                .toList();
    }
}
