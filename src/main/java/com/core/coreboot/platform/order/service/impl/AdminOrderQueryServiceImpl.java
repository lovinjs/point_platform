package com.core.coreboot.platform.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.core.coreboot.exception.CustomException;
import com.core.coreboot.exception.ExceptionEnum;
import com.core.coreboot.platform.common.enums.ConsumptionOrderStatus;
import com.core.coreboot.platform.common.enums.RechargeOrderStatus;
import com.core.coreboot.platform.common.enums.RoleCode;
import com.core.coreboot.platform.common.enums.SysUserStatus;
import com.core.coreboot.platform.common.model.PageResult;
import com.core.coreboot.platform.consumption.entity.ConsumptionOrder;
import com.core.coreboot.platform.consumption.mapper.ConsumptionOrderMapper;
import com.core.coreboot.platform.customer.entity.CustomerUser;
import com.core.coreboot.platform.customer.mapper.CustomerUserMapper;
import com.core.coreboot.platform.merchant.entity.Store;
import com.core.coreboot.platform.merchant.mapper.StoreMapper;
import com.core.coreboot.platform.order.model.AdminConsumptionOrderView;
import com.core.coreboot.platform.order.model.AdminOrderStoreOptionView;
import com.core.coreboot.platform.order.model.AdminRechargeOrderView;
import com.core.coreboot.platform.order.service.AdminOrderQueryService;
import com.core.coreboot.platform.recharge.entity.RechargeOrder;
import com.core.coreboot.platform.recharge.entity.RechargeRefund;
import com.core.coreboot.platform.recharge.mapper.RechargeOrderMapper;
import com.core.coreboot.platform.recharge.mapper.RechargeRefundMapper;
import com.core.coreboot.platform.staff.entity.SysUser;
import com.core.coreboot.platform.staff.mapper.StaffAuthorityMapper;
import com.core.coreboot.platform.staff.mapper.SysUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class AdminOrderQueryServiceImpl implements AdminOrderQueryService {
    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_EXPORT_DAYS = 93;
    private static final int MAX_EXPORT_ROWS = 10_000;
    private static final int MAX_ORDER_NO_LENGTH = 64;
    private static final Pattern PHONE_PATTERN = Pattern.compile("\\+?[0-9]{6,20}");

    private final RechargeOrderMapper rechargeOrderMapper;
    private final RechargeRefundMapper rechargeRefundMapper;
    private final ConsumptionOrderMapper consumptionOrderMapper;
    private final CustomerUserMapper customerUserMapper;
    private final StoreMapper storeMapper;
    private final SysUserMapper sysUserMapper;
    private final StaffAuthorityMapper staffAuthorityMapper;
    private final Clock clock;

    @Autowired
    public AdminOrderQueryServiceImpl(
            RechargeOrderMapper rechargeOrderMapper,
            RechargeRefundMapper rechargeRefundMapper,
            ConsumptionOrderMapper consumptionOrderMapper,
            CustomerUserMapper customerUserMapper,
            StoreMapper storeMapper,
            SysUserMapper sysUserMapper,
            StaffAuthorityMapper staffAuthorityMapper
    ) {
        this(
                rechargeOrderMapper,
                rechargeRefundMapper,
                consumptionOrderMapper,
                customerUserMapper,
                storeMapper,
                sysUserMapper,
                staffAuthorityMapper,
                Clock.systemDefaultZone()
        );
    }

    AdminOrderQueryServiceImpl(
            RechargeOrderMapper rechargeOrderMapper,
            RechargeRefundMapper rechargeRefundMapper,
            ConsumptionOrderMapper consumptionOrderMapper,
            CustomerUserMapper customerUserMapper,
            StoreMapper storeMapper,
            SysUserMapper sysUserMapper,
            StaffAuthorityMapper staffAuthorityMapper,
            Clock clock
    ) {
        this.rechargeOrderMapper = rechargeOrderMapper;
        this.rechargeRefundMapper = rechargeRefundMapper;
        this.consumptionOrderMapper = consumptionOrderMapper;
        this.customerUserMapper = customerUserMapper;
        this.storeMapper = storeMapper;
        this.sysUserMapper = sysUserMapper;
        this.staffAuthorityMapper = staffAuthorityMapper;
        this.clock = clock;
    }

    @Override
    public List<AdminOrderStoreOptionView> listStoreOptions(Long operatorId) {
        OperatorScope scope = requireScope(operatorId);
        List<Store> stores;
        if (scope.unrestricted()) {
            stores = storeMapper.selectList(Wrappers.lambdaQuery(Store.class)
                    .orderByAsc(Store::getStoreName)
                    .orderByAsc(Store::getId));
        } else if (scope.storeIds().isEmpty()) {
            stores = List.of();
        } else {
            stores = storeMapper.selectByIds(scope.storeIds());
        }
        return stores.stream()
                .sorted(Comparator
                        .comparing(Store::getStoreName, Comparator.nullsLast(String::compareTo))
                        .thenComparing(Store::getId))
                .map(store -> new AdminOrderStoreOptionView(
                        store.getId(),
                        store.getStoreCode(),
                        store.getStoreName(),
                        store.getStatus()
                ))
                .toList();
    }

    @Override
    public PageResult<AdminRechargeOrderView> listRechargeOrders(
            Long operatorId,
            int pageNum,
            int pageSize,
            Long storeId,
            RechargeOrderStatus status,
            String orderNo,
            String customerPhone
    ) {
        OperatorScope scope = requireScope(operatorId);
        validatePage(pageNum, pageSize);
        validateRequestedStore(scope, storeId);
        String normalizedOrderNo = normalizeOrderNo(orderNo);
        CustomerFilter customerFilter = resolveCustomer(customerPhone);
        if (customerFilter.supplied() && customerFilter.customerId() == null) {
            return emptyPage(pageNum, pageSize);
        }
        if (!scope.unrestricted() && scope.storeIds().isEmpty()) {
            return emptyPage(pageNum, pageSize);
        }

        LambdaQueryWrapper<RechargeOrder> wrapper = Wrappers.lambdaQuery(RechargeOrder.class)
                .eq(storeId != null, RechargeOrder::getRechargeStoreId, storeId)
                .eq(status != null, RechargeOrder::getOrderStatus, status)
                .like(normalizedOrderNo != null, RechargeOrder::getOrderNo, normalizedOrderNo)
                .eq(customerFilter.customerId() != null, RechargeOrder::getCustomerId, customerFilter.customerId());
        if (storeId == null && !scope.unrestricted()) {
            wrapper.in(RechargeOrder::getRechargeStoreId, scope.storeIds());
        }
        wrapper.orderByDesc(RechargeOrder::getCreateTime).orderByDesc(RechargeOrder::getId);

        IPage<RechargeOrder> page = rechargeOrderMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        RelatedContext context = loadContext(page.getRecords().stream().map(RechargeOrder::getCustomerId).toList(),
                page.getRecords().stream().map(RechargeOrder::getRechargeStoreId).toList(),
                page.getRecords().stream().map(RechargeOrder::getOperatorId).toList());
        Map<Long, RechargeRefund> refunds = loadRefunds(page.getRecords());
        List<AdminRechargeOrderView> items = page.getRecords().stream()
                .map(order -> toRechargeView(order, refunds.get(order.getId()), context))
                .toList();
        return toPageResult(page, items);
    }

    @Override
    public PageResult<AdminConsumptionOrderView> listConsumptionOrders(
            Long operatorId,
            int pageNum,
            int pageSize,
            Long storeId,
            ConsumptionOrderStatus status,
            String orderNo,
            String customerPhone
    ) {
        OperatorScope scope = requireScope(operatorId);
        validatePage(pageNum, pageSize);
        validateRequestedStore(scope, storeId);
        String normalizedOrderNo = normalizeOrderNo(orderNo);
        CustomerFilter customerFilter = resolveCustomer(customerPhone);
        if (customerFilter.supplied() && customerFilter.customerId() == null) {
            return emptyPage(pageNum, pageSize);
        }
        if (!scope.unrestricted() && scope.storeIds().isEmpty()) {
            return emptyPage(pageNum, pageSize);
        }

        LocalDateTime now = LocalDateTime.now(clock);
        LambdaQueryWrapper<ConsumptionOrder> wrapper = Wrappers.lambdaQuery(ConsumptionOrder.class)
                .eq(storeId != null, ConsumptionOrder::getStoreId, storeId)
                .like(normalizedOrderNo != null, ConsumptionOrder::getOrderNo, normalizedOrderNo)
                .eq(customerFilter.customerId() != null, ConsumptionOrder::getCustomerId, customerFilter.customerId());
        applyConsumptionStatus(wrapper, status, now);
        if (storeId == null && !scope.unrestricted()) {
            wrapper.in(ConsumptionOrder::getStoreId, scope.storeIds());
        }
        wrapper.orderByDesc(ConsumptionOrder::getCreateTime).orderByDesc(ConsumptionOrder::getId);

        IPage<ConsumptionOrder> page = consumptionOrderMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        RelatedContext context = loadContext(page.getRecords().stream().map(ConsumptionOrder::getCustomerId).toList(),
                page.getRecords().stream().map(ConsumptionOrder::getStoreId).toList(),
                page.getRecords().stream().map(ConsumptionOrder::getOperatorId).toList());
        List<AdminConsumptionOrderView> items = page.getRecords().stream()
                .map(order -> toConsumptionView(order, context, now))
                .toList();
        return toPageResult(page, items);
    }

    @Override
    public List<AdminRechargeOrderView> exportRechargeOrders(
            Long operatorId,
            LocalDate startDate,
            LocalDate endDate,
            Long storeId,
            RechargeOrderStatus status,
            String orderNo,
            String customerPhone
    ) {
        OperatorScope scope = requireScope(operatorId, true);
        validateRequestedStore(scope, storeId);
        DateRange range = validateExportDateRange(startDate, endDate);
        String normalizedOrderNo = normalizeOrderNo(orderNo);
        CustomerFilter customerFilter = resolveCustomer(customerPhone);
        if (customerFilter.supplied() && customerFilter.customerId() == null) {
            return List.of();
        }
        if (!scope.unrestricted() && scope.storeIds().isEmpty()) {
            return List.of();
        }

        LambdaQueryWrapper<RechargeOrder> wrapper = Wrappers.lambdaQuery(RechargeOrder.class)
                .ge(RechargeOrder::getCreateTime, range.startTime())
                .lt(RechargeOrder::getCreateTime, range.endTimeExclusive())
                .eq(storeId != null, RechargeOrder::getRechargeStoreId, storeId)
                .eq(status != null, RechargeOrder::getOrderStatus, status)
                .like(normalizedOrderNo != null, RechargeOrder::getOrderNo, normalizedOrderNo)
                .eq(customerFilter.customerId() != null, RechargeOrder::getCustomerId, customerFilter.customerId());
        if (storeId == null && !scope.unrestricted()) {
            wrapper.in(RechargeOrder::getRechargeStoreId, scope.storeIds());
        }
        wrapper.orderByDesc(RechargeOrder::getCreateTime)
                .orderByDesc(RechargeOrder::getId)
                .last("LIMIT " + (MAX_EXPORT_ROWS + 1));

        List<RechargeOrder> orders = rechargeOrderMapper.selectList(wrapper);
        requireExportRowLimit(orders.size());
        RelatedContext context = loadContext(
                orders.stream().map(RechargeOrder::getCustomerId).toList(),
                orders.stream().map(RechargeOrder::getRechargeStoreId).toList(),
                orders.stream().map(RechargeOrder::getOperatorId).toList()
        );
        Map<Long, RechargeRefund> refunds = loadRefunds(orders);
        return orders.stream()
                .map(order -> toRechargeView(order, refunds.get(order.getId()), context))
                .toList();
    }

    @Override
    public List<AdminConsumptionOrderView> exportConsumptionOrders(
            Long operatorId,
            LocalDate startDate,
            LocalDate endDate,
            Long storeId,
            ConsumptionOrderStatus status,
            String orderNo,
            String customerPhone
    ) {
        OperatorScope scope = requireScope(operatorId, true);
        validateRequestedStore(scope, storeId);
        DateRange range = validateExportDateRange(startDate, endDate);
        String normalizedOrderNo = normalizeOrderNo(orderNo);
        CustomerFilter customerFilter = resolveCustomer(customerPhone);
        if (customerFilter.supplied() && customerFilter.customerId() == null) {
            return List.of();
        }
        if (!scope.unrestricted() && scope.storeIds().isEmpty()) {
            return List.of();
        }

        LocalDateTime now = LocalDateTime.now(clock);
        LambdaQueryWrapper<ConsumptionOrder> wrapper = Wrappers.lambdaQuery(ConsumptionOrder.class)
                .ge(ConsumptionOrder::getCreateTime, range.startTime())
                .lt(ConsumptionOrder::getCreateTime, range.endTimeExclusive())
                .eq(storeId != null, ConsumptionOrder::getStoreId, storeId)
                .like(normalizedOrderNo != null, ConsumptionOrder::getOrderNo, normalizedOrderNo)
                .eq(customerFilter.customerId() != null, ConsumptionOrder::getCustomerId, customerFilter.customerId());
        applyConsumptionStatus(wrapper, status, now);
        if (storeId == null && !scope.unrestricted()) {
            wrapper.in(ConsumptionOrder::getStoreId, scope.storeIds());
        }
        wrapper.orderByDesc(ConsumptionOrder::getCreateTime)
                .orderByDesc(ConsumptionOrder::getId)
                .last("LIMIT " + (MAX_EXPORT_ROWS + 1));

        List<ConsumptionOrder> orders = consumptionOrderMapper.selectList(wrapper);
        requireExportRowLimit(orders.size());
        RelatedContext context = loadContext(
                orders.stream().map(ConsumptionOrder::getCustomerId).toList(),
                orders.stream().map(ConsumptionOrder::getStoreId).toList(),
                orders.stream().map(ConsumptionOrder::getOperatorId).toList()
        );
        return orders.stream()
                .map(order -> toConsumptionView(order, context, now))
                .toList();
    }

    private void applyConsumptionStatus(
            LambdaQueryWrapper<ConsumptionOrder> wrapper,
            ConsumptionOrderStatus status,
            LocalDateTime now
    ) {
        if (status == null) {
            return;
        }
        if (status == ConsumptionOrderStatus.EXPIRED) {
            wrapper.and(condition -> condition
                    .eq(ConsumptionOrder::getOrderStatus, ConsumptionOrderStatus.EXPIRED)
                    .or(expired -> expired
                            .eq(ConsumptionOrder::getOrderStatus, ConsumptionOrderStatus.PENDING_CONFIRM)
                            .le(ConsumptionOrder::getExpiresTime, now)));
            return;
        }
        if (status == ConsumptionOrderStatus.PENDING_CONFIRM) {
            wrapper.eq(ConsumptionOrder::getOrderStatus, ConsumptionOrderStatus.PENDING_CONFIRM)
                    .gt(ConsumptionOrder::getExpiresTime, now);
            return;
        }
        wrapper.eq(ConsumptionOrder::getOrderStatus, status);
    }

    private OperatorScope requireScope(Long operatorId) {
        return requireScope(operatorId, false);
    }

    private OperatorScope requireScope(Long operatorId, boolean export) {
        if (operatorId == null || operatorId <= 0) {
            throw new CustomException(ExceptionEnum.PLATFORM_ADMIN_TOKEN_INVALID);
        }
        SysUser operator = sysUserMapper.selectById(operatorId);
        if (operator == null) {
            throw new CustomException(ExceptionEnum.PLATFORM_OPERATOR_NOT_FOUND);
        }
        if (operator.getStatus() != SysUserStatus.ACTIVE) {
            throw new CustomException(ExceptionEnum.PLATFORM_OPERATOR_DISABLED);
        }
        List<String> roles = staffAuthorityMapper.selectRoleCodes(operatorId);
        if (roles == null) {
            roles = List.of();
        }
        if (roles.contains(RoleCode.SUPER_ADMIN.getCode())) {
            return new OperatorScope(true, List.of());
        }
        boolean isStoreStaff = roles.contains(RoleCode.STORE_MANAGER.getCode())
                || (!export && roles.contains(RoleCode.CLERK.getCode()));
        if (!isStoreStaff) {
            throw new CustomException(ExceptionEnum.PLATFORM_ADMIN_ACCESS_DENIED);
        }
        List<Long> storeIds = staffAuthorityMapper.selectStoreIds(operatorId);
        if (storeIds == null) {
            storeIds = List.of();
        }
        return new OperatorScope(false, storeIds.stream()
                .filter(Objects::nonNull)
                .filter(id -> id > 0)
                .distinct()
                .toList());
    }

    private void validateRequestedStore(OperatorScope scope, Long storeId) {
        if (storeId != null && storeId <= 0) {
            throw new CustomException(ExceptionEnum.PLATFORM_INVALID_REQUEST);
        }
        if (storeId != null && !scope.unrestricted() && !scope.storeIds().contains(storeId)) {
            throw new CustomException(ExceptionEnum.PLATFORM_STORE_ACCESS_DENIED);
        }
    }

    private CustomerFilter resolveCustomer(String customerPhone) {
        String normalizedPhone = normalizeOptional(customerPhone);
        if (normalizedPhone == null) {
            return new CustomerFilter(false, null);
        }
        if (!PHONE_PATTERN.matcher(normalizedPhone).matches()) {
            throw new CustomException(ExceptionEnum.PLATFORM_INVALID_REQUEST);
        }
        CustomerUser customer = customerUserMapper.selectByPhone(normalizedPhone);
        return new CustomerFilter(true, customer == null ? null : customer.getId());
    }

    private RelatedContext loadContext(
            Collection<Long> customerIds,
            Collection<Long> storeIds,
            Collection<Long> operatorIds
    ) {
        return new RelatedContext(
                loadCustomers(customerIds),
                loadStores(storeIds),
                loadOperators(operatorIds)
        );
    }

    private Map<Long, CustomerUser> loadCustomers(Collection<Long> ids) {
        List<Long> validIds = distinctIds(ids);
        if (validIds.isEmpty()) {
            return Map.of();
        }
        return customerUserMapper.selectByIds(validIds).stream()
                .collect(Collectors.toMap(CustomerUser::getId, Function.identity()));
    }

    private Map<Long, Store> loadStores(Collection<Long> ids) {
        List<Long> validIds = distinctIds(ids);
        if (validIds.isEmpty()) {
            return Map.of();
        }
        return storeMapper.selectByIds(validIds).stream()
                .collect(Collectors.toMap(Store::getId, Function.identity()));
    }

    private Map<Long, SysUser> loadOperators(Collection<Long> ids) {
        List<Long> validIds = distinctIds(ids);
        if (validIds.isEmpty()) {
            return Map.of();
        }
        return sysUserMapper.selectByIds(validIds).stream()
                .collect(Collectors.toMap(SysUser::getId, Function.identity()));
    }

    private Map<Long, RechargeRefund> loadRefunds(List<RechargeOrder> orders) {
        List<Long> orderIds = orders.stream()
                .map(RechargeOrder::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (orderIds.isEmpty()) {
            return Map.of();
        }
        return rechargeRefundMapper.selectList(Wrappers.lambdaQuery(RechargeRefund.class)
                        .in(RechargeRefund::getRechargeOrderId, orderIds))
                .stream()
                .collect(Collectors.toMap(
                        RechargeRefund::getRechargeOrderId,
                        Function.identity(),
                        (first, ignored) -> first
                ));
    }

    private AdminRechargeOrderView toRechargeView(
            RechargeOrder order,
            RechargeRefund refund,
            RelatedContext context
    ) {
        if (order.getRechargePoints() == null || order.getRechargePoints() <= 0
                || order.getAmountCent() == null || order.getAmountCent() <= 0
                || order.getOrderStatus() == null) {
            throw new CustomException(ExceptionEnum.PLATFORM_DATA_CONFLICT);
        }
        CustomerUser customer = context.customers().get(order.getCustomerId());
        Store store = context.stores().get(order.getRechargeStoreId());
        return new AdminRechargeOrderView(
                order.getOrderNo(),
                order.getCustomerId(),
                customer == null ? null : customer.getPhone(),
                customer == null ? null : customer.getNickname(),
                order.getRechargeStoreId(),
                store == null ? null : store.getStoreCode(),
                store == null ? null : store.getStoreName(),
                order.getRechargePoints(),
                order.getAmountCent(),
                order.getChannel(),
                order.getPaymentMethod(),
                order.getFundReceiver(),
                order.getPaymentReference(),
                order.getOrderStatus(),
                order.getOperatorId(),
                operatorName(context.operators().get(order.getOperatorId())),
                order.getRemark(),
                order.getPaidTime(),
                order.getCompletedTime(),
                order.getCreateTime(),
                refund == null ? null : refund.getRefundNo(),
                refund == null ? null : refund.getRefundMethod(),
                refund == null ? null : refund.getRefundReference(),
                refund == null ? null : refund.getRefundStatus(),
                refund == null ? null : refund.getReason(),
                refund == null ? null : refund.getCompletedTime()
        );
    }

    private AdminConsumptionOrderView toConsumptionView(
            ConsumptionOrder order,
            RelatedContext context,
            LocalDateTime now
    ) {
        if (order.getConsumePoints() == null || order.getConsumePoints() <= 0
                || order.getGrossAmountCent() == null || order.getGrossAmountCent() < 0
                || order.getPlatformFeeRateBps() == null || order.getPlatformFeeRateBps() < 0
                || order.getPlatformFeeCent() == null || order.getPlatformFeeCent() < 0
                || order.getStorePayableCent() == null || order.getStorePayableCent() < 0
                || order.getVerificationMode() == null || order.getOrderStatus() == null) {
            throw new CustomException(ExceptionEnum.PLATFORM_DATA_CONFLICT);
        }
        CustomerUser customer = context.customers().get(order.getCustomerId());
        Store store = context.stores().get(order.getStoreId());
        ConsumptionOrderStatus effectiveStatus = isLogicallyExpired(order, now)
                ? ConsumptionOrderStatus.EXPIRED
                : order.getOrderStatus();
        return new AdminConsumptionOrderView(
                order.getOrderNo(),
                order.getCustomerId(),
                customer == null ? null : customer.getPhone(),
                customer == null ? null : customer.getNickname(),
                order.getStoreId(),
                store == null ? null : store.getStoreCode(),
                store == null ? null : store.getStoreName(),
                order.getConsumePoints(),
                order.getGrossAmountCent(),
                order.getPlatformFeeRateBps(),
                order.getPlatformFeeCent(),
                order.getStorePayableCent(),
                order.getVerificationMode(),
                effectiveStatus,
                order.getSettlementStatus(),
                order.getOperatorId(),
                operatorName(context.operators().get(order.getOperatorId())),
                order.getRemark(),
                order.getExpiresTime(),
                order.getConfirmedTime(),
                order.getCompletedTime(),
                order.getCreateTime()
        );
    }

    private boolean isLogicallyExpired(ConsumptionOrder order, LocalDateTime now) {
        return order.getOrderStatus() == ConsumptionOrderStatus.PENDING_CONFIRM
                && order.getExpiresTime() != null
                && !order.getExpiresTime().isAfter(now);
    }

    private String operatorName(SysUser operator) {
        if (operator == null) {
            return null;
        }
        String realName = normalizeOptional(operator.getRealName());
        return realName == null ? operator.getUsername() : realName;
    }

    private List<Long> distinctIds(Collection<Long> ids) {
        return ids.stream()
                .filter(Objects::nonNull)
                .filter(id -> id > 0)
                .distinct()
                .toList();
    }

    private String normalizeOrderNo(String orderNo) {
        String normalized = normalizeOptional(orderNo);
        if (normalized != null && normalized.length() > MAX_ORDER_NO_LENGTH) {
            throw new CustomException(ExceptionEnum.PLATFORM_INVALID_REQUEST);
        }
        return normalized;
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private void validatePage(int pageNum, int pageSize) {
        if (pageNum < 1 || pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw new CustomException(ExceptionEnum.PLATFORM_INVALID_REQUEST);
        }
    }

    private DateRange validateExportDateRange(LocalDate startDate, LocalDate endDate) {
        LocalDate today = LocalDate.now(clock);
        if (startDate == null || endDate == null
                || endDate.isBefore(startDate)
                || endDate.isAfter(today)
                || ChronoUnit.DAYS.between(startDate, endDate) >= MAX_EXPORT_DAYS) {
            throw new CustomException(ExceptionEnum.PLATFORM_INVALID_REQUEST);
        }
        return new DateRange(startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay());
    }

    private void requireExportRowLimit(int rowCount) {
        if (rowCount > MAX_EXPORT_ROWS) {
            throw new CustomException(ExceptionEnum.PLATFORM_REPORT_ROW_LIMIT_EXCEEDED);
        }
    }

    private <T> PageResult<T> emptyPage(int pageNum, int pageSize) {
        return new PageResult<>(pageNum, pageSize, 0, 0, false, List.of());
    }

    private <S, T> PageResult<T> toPageResult(IPage<S> page, List<T> items) {
        return new PageResult<>(
                page.getCurrent(),
                page.getSize(),
                page.getTotal(),
                page.getPages(),
                page.getCurrent() < page.getPages(),
                items
        );
    }

    private record OperatorScope(boolean unrestricted, List<Long> storeIds) {
    }

    private record CustomerFilter(boolean supplied, Long customerId) {
    }

    private record RelatedContext(
            Map<Long, CustomerUser> customers,
            Map<Long, Store> stores,
            Map<Long, SysUser> operators
    ) {
    }

    private record DateRange(LocalDateTime startTime, LocalDateTime endTimeExclusive) {
    }
}
