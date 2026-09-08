package com.core.coreboot.platform.customer.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.core.coreboot.exception.CustomException;
import com.core.coreboot.exception.ExceptionEnum;
import com.core.coreboot.platform.common.model.PageResult;
import com.core.coreboot.platform.consumption.entity.ConsumptionOrder;
import com.core.coreboot.platform.consumption.mapper.ConsumptionOrderMapper;
import com.core.coreboot.platform.consumption.model.CustomerConsumptionOrderView;
import com.core.coreboot.platform.customer.service.CustomerTransactionQueryService;
import com.core.coreboot.platform.merchant.entity.Store;
import com.core.coreboot.platform.merchant.mapper.StoreMapper;
import com.core.coreboot.platform.point.entity.PointAccount;
import com.core.coreboot.platform.point.entity.PointLedger;
import com.core.coreboot.platform.point.mapper.PointAccountMapper;
import com.core.coreboot.platform.point.mapper.PointLedgerMapper;
import com.core.coreboot.platform.point.model.CustomerPointBalanceView;
import com.core.coreboot.platform.point.model.CustomerPointLedgerView;
import com.core.coreboot.platform.recharge.entity.RechargeOrder;
import com.core.coreboot.platform.recharge.mapper.RechargeOrderMapper;
import com.core.coreboot.platform.recharge.model.CustomerRechargeOrderView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CustomerTransactionQueryServiceImpl implements CustomerTransactionQueryService {
    private static final int MAX_PAGE_SIZE = 50;

    private final PointAccountMapper pointAccountMapper;
    private final PointLedgerMapper pointLedgerMapper;
    private final RechargeOrderMapper rechargeOrderMapper;
    private final ConsumptionOrderMapper consumptionOrderMapper;
    private final StoreMapper storeMapper;
    private final Clock clock;

    @Autowired
    public CustomerTransactionQueryServiceImpl(
            PointAccountMapper pointAccountMapper,
            PointLedgerMapper pointLedgerMapper,
            RechargeOrderMapper rechargeOrderMapper,
            ConsumptionOrderMapper consumptionOrderMapper,
            StoreMapper storeMapper
    ) {
        this(
                pointAccountMapper,
                pointLedgerMapper,
                rechargeOrderMapper,
                consumptionOrderMapper,
                storeMapper,
                Clock.systemDefaultZone()
        );
    }

    CustomerTransactionQueryServiceImpl(
            PointAccountMapper pointAccountMapper,
            PointLedgerMapper pointLedgerMapper,
            RechargeOrderMapper rechargeOrderMapper,
            ConsumptionOrderMapper consumptionOrderMapper,
            StoreMapper storeMapper,
            Clock clock
    ) {
        this.pointAccountMapper = pointAccountMapper;
        this.pointLedgerMapper = pointLedgerMapper;
        this.rechargeOrderMapper = rechargeOrderMapper;
        this.consumptionOrderMapper = consumptionOrderMapper;
        this.storeMapper = storeMapper;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerPointBalanceView getPointBalance(Long customerId) {
        requireCustomerId(customerId);
        PointAccount account = pointAccountMapper.selectByCustomerId(customerId);
        if (account == null) {
            return new CustomerPointBalanceView(0L, null);
        }
        return new CustomerPointBalanceView(
                requireNonNegative(account.getAvailablePoints()),
                account.getUpdateTime()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<CustomerPointLedgerView> getPointLedger(
            Long customerId,
            int pageNum,
            int pageSize
    ) {
        requireCustomerId(customerId);
        Page<PointLedger> request = pageRequest(pageNum, pageSize);
        IPage<PointLedger> page = pointLedgerMapper.selectPage(
                request,
                Wrappers.lambdaQuery(PointLedger.class)
                        .eq(PointLedger::getCustomerId, customerId)
                        .orderByDesc(PointLedger::getCreateTime)
                        .orderByDesc(PointLedger::getId)
        );
        Map<Long, String> storeNames = loadStoreNames(
                page.getRecords().stream().map(PointLedger::getStoreId).toList()
        );
        return mapPage(page, item -> new CustomerPointLedgerView(
                item.getLedgerNo(),
                requireSigned(item.getDeltaPoints()),
                requireNonNegative(item.getBalanceAfter()),
                item.getLedgerType(),
                item.getBusinessType(),
                item.getBusinessNo(),
                item.getStoreId(),
                storeNames.get(item.getStoreId()),
                item.getRemark(),
                item.getCreateTime()
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<CustomerRechargeOrderView> getRechargeOrders(
            Long customerId,
            int pageNum,
            int pageSize
    ) {
        requireCustomerId(customerId);
        Page<RechargeOrder> request = pageRequest(pageNum, pageSize);
        IPage<RechargeOrder> page = rechargeOrderMapper.selectPage(
                request,
                Wrappers.lambdaQuery(RechargeOrder.class)
                        .eq(RechargeOrder::getCustomerId, customerId)
                        .orderByDesc(RechargeOrder::getCreateTime)
                        .orderByDesc(RechargeOrder::getId)
        );
        Map<Long, String> storeNames = loadStoreNames(
                page.getRecords().stream().map(RechargeOrder::getRechargeStoreId).toList()
        );
        return mapPage(page, item -> new CustomerRechargeOrderView(
                item.getOrderNo(),
                item.getRechargeStoreId(),
                storeNames.get(item.getRechargeStoreId()),
                requirePositive(item.getRechargePoints()),
                requireNonNegative(item.getAmountCent()),
                item.getChannel(),
                item.getPaymentMethod(),
                item.getOrderStatus(),
                item.getRemark(),
                item.getPaidTime(),
                item.getCompletedTime(),
                item.getCreateTime()
        ));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PageResult<CustomerConsumptionOrderView> getConsumptionOrders(
            Long customerId,
            int pageNum,
            int pageSize
    ) {
        requireCustomerId(customerId);
        consumptionOrderMapper.expirePendingByCustomerId(customerId, LocalDateTime.now(clock));
        Page<ConsumptionOrder> request = pageRequest(pageNum, pageSize);
        IPage<ConsumptionOrder> page = consumptionOrderMapper.selectPage(
                request,
                Wrappers.lambdaQuery(ConsumptionOrder.class)
                        .eq(ConsumptionOrder::getCustomerId, customerId)
                        .orderByDesc(ConsumptionOrder::getCreateTime)
                        .orderByDesc(ConsumptionOrder::getId)
        );
        Map<Long, String> storeNames = loadStoreNames(
                page.getRecords().stream().map(ConsumptionOrder::getStoreId).toList()
        );
        return mapPage(page, item -> new CustomerConsumptionOrderView(
                item.getOrderNo(),
                item.getStoreId(),
                storeNames.get(item.getStoreId()),
                requirePositive(item.getConsumePoints()),
                requireNonNegative(item.getGrossAmountCent()),
                item.getOrderStatus(),
                item.getRemark(),
                item.getExpiresTime(),
                item.getConfirmedTime(),
                item.getCompletedTime(),
                item.getCreateTime()
        ));
    }

    private <T> Page<T> pageRequest(int pageNum, int pageSize) {
        if (pageNum < 1 || pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw new CustomException(ExceptionEnum.PLATFORM_INVALID_REQUEST);
        }
        return new Page<>(pageNum, pageSize);
    }

    private Map<Long, String> loadStoreNames(Collection<Long> storeIds) {
        LinkedHashSet<Long> normalizedIds = storeIds == null
                ? new LinkedHashSet<>()
                : storeIds.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (normalizedIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Store> stores = storeMapper.selectList(
                Wrappers.lambdaQuery(Store.class)
                        .select(Store::getId, Store::getStoreName)
                        .in(Store::getId, normalizedIds)
        );
        if (stores == null || stores.isEmpty()) {
            return Collections.emptyMap();
        }
        return stores.stream().collect(Collectors.toMap(
                Store::getId,
                Store::getStoreName,
                (left, right) -> left
        ));
    }

    private <S, T> PageResult<T> mapPage(IPage<S> page, Function<S, T> mapper) {
        List<T> items = page.getRecords().stream().map(mapper).toList();
        return new PageResult<>(
                page.getCurrent(),
                page.getSize(),
                page.getTotal(),
                page.getPages(),
                page.getCurrent() < page.getPages(),
                items
        );
    }

    private void requireCustomerId(Long customerId) {
        if (customerId == null || customerId <= 0) {
            throw new CustomException(ExceptionEnum.PLATFORM_CUSTOMER_NOT_FOUND);
        }
    }

    private long requirePositive(Long value) {
        if (value == null || value <= 0) {
            throw new CustomException(ExceptionEnum.PLATFORM_POINT_ACCOUNT_ERROR);
        }
        return value;
    }

    private long requireNonNegative(Long value) {
        if (value == null || value < 0) {
            throw new CustomException(ExceptionEnum.PLATFORM_POINT_ACCOUNT_ERROR);
        }
        return value;
    }

    private long requireSigned(Long value) {
        if (value == null || value == 0) {
            throw new CustomException(ExceptionEnum.PLATFORM_POINT_ACCOUNT_ERROR);
        }
        return value;
    }
}
