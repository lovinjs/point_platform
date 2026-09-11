package com.core.coreboot.platform.merchant.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.core.coreboot.exception.CustomException;
import com.core.coreboot.exception.ExceptionEnum;
import com.core.coreboot.platform.common.enums.MerchantStatus;
import com.core.coreboot.platform.common.enums.StoreStatus;
import com.core.coreboot.platform.common.model.PageResult;
import com.core.coreboot.platform.merchant.entity.Merchant;
import com.core.coreboot.platform.merchant.entity.Store;
import com.core.coreboot.platform.merchant.mapper.MerchantMapper;
import com.core.coreboot.platform.merchant.mapper.StoreMapper;
import com.core.coreboot.platform.merchant.model.CustomerStoreView;
import com.core.coreboot.platform.merchant.service.CustomerStoreQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerStoreQueryServiceImpl implements CustomerStoreQueryService {
    private static final int MAX_PAGE_SIZE = 50;
    private static final int MAX_KEYWORD_LENGTH = 100;
    private static final String ACTIVE_MERCHANT_IDS_SQL =
            "SELECT id FROM t_merchant WHERE status = 'ACTIVE'";

    private final StoreMapper storeMapper;
    private final MerchantMapper merchantMapper;

    @Override
    public PageResult<CustomerStoreView> list(int pageNum, int pageSize, String keyword) {
        validatePage(pageNum, pageSize);
        String normalizedKeyword = normalizeOptional(keyword);
        if (normalizedKeyword != null && normalizedKeyword.length() > MAX_KEYWORD_LENGTH) {
            throw new CustomException(ExceptionEnum.PLATFORM_INVALID_REQUEST);
        }

        IPage<Store> page = storeMapper.selectPage(
                new Page<>(pageNum, pageSize),
                Wrappers.lambdaQuery(Store.class)
                        .eq(Store::getStatus, StoreStatus.ACTIVE)
                        .inSql(Store::getMerchantId, ACTIVE_MERCHANT_IDS_SQL)
                        .and(normalizedKeyword != null, condition -> condition
                                .like(Store::getStoreName, normalizedKeyword)
                                .or()
                                .like(Store::getAddress, normalizedKeyword))
                        .orderByAsc(Store::getStoreName)
                        .orderByAsc(Store::getId)
        );
        Map<Long, Merchant> merchants = loadMerchants(
                page.getRecords().stream().map(Store::getMerchantId).toList()
        );
        List<CustomerStoreView> items = page.getRecords().stream()
                .map(store -> toView(store, merchants.get(store.getMerchantId())))
                .toList();
        return new PageResult<>(
                page.getCurrent(),
                page.getSize(),
                page.getTotal(),
                page.getPages(),
                page.getCurrent() < page.getPages(),
                items
        );
    }

    @Override
    public CustomerStoreView getById(Long storeId) {
        if (storeId == null || storeId <= 0) {
            throw new CustomException(ExceptionEnum.PLATFORM_INVALID_REQUEST);
        }
        Store store = storeMapper.selectById(storeId);
        if (store == null || store.getStatus() != StoreStatus.ACTIVE) {
            throw new CustomException(ExceptionEnum.PLATFORM_STORE_NOT_FOUND);
        }
        Merchant merchant = merchantMapper.selectById(store.getMerchantId());
        if (merchant == null || merchant.getStatus() != MerchantStatus.ACTIVE) {
            throw new CustomException(ExceptionEnum.PLATFORM_STORE_NOT_FOUND);
        }
        return toView(store, merchant);
    }

    private Map<Long, Merchant> loadMerchants(Collection<Long> merchantIds) {
        List<Long> distinctIds = merchantIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (distinctIds.isEmpty()) {
            return Map.of();
        }
        return merchantMapper.selectByIds(distinctIds).stream()
                .filter(merchant -> merchant.getStatus() == MerchantStatus.ACTIVE)
                .collect(Collectors.toMap(Merchant::getId, Function.identity()));
    }

    private CustomerStoreView toView(Store store, Merchant merchant) {
        if (merchant == null || !Objects.equals(store.getMerchantId(), merchant.getId())) {
            throw new CustomException(ExceptionEnum.PLATFORM_DATA_CONFLICT);
        }
        return new CustomerStoreView(
                store.getId(),
                store.getStoreCode(),
                store.getStoreName(),
                merchant.getId(),
                merchant.getBusinessName(),
                store.getAddress(),
                store.getContactPhone()
        );
    }

    private void validatePage(int pageNum, int pageSize) {
        if (pageNum < 1 || pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw new CustomException(ExceptionEnum.PLATFORM_INVALID_REQUEST);
        }
    }

    private String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
