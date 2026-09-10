package com.core.coreboot.platform.finance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.core.coreboot.exception.CustomException;
import com.core.coreboot.exception.ExceptionEnum;
import com.core.coreboot.platform.audit.entity.AuditLog;
import com.core.coreboot.platform.audit.mapper.AuditLogMapper;
import com.core.coreboot.platform.common.enums.AuditActorType;
import com.core.coreboot.platform.common.enums.RoleCode;
import com.core.coreboot.platform.common.enums.SysUserStatus;
import com.core.coreboot.platform.common.model.PageResult;
import com.core.coreboot.platform.customer.entity.CustomerUser;
import com.core.coreboot.platform.customer.mapper.CustomerUserMapper;
import com.core.coreboot.platform.finance.mapper.FinancialReconciliationMapper;
import com.core.coreboot.platform.finance.model.AdminAuditLogView;
import com.core.coreboot.platform.finance.model.AdminFinancialReconciliationView;
import com.core.coreboot.platform.finance.model.AdminFinancialStoreView;
import com.core.coreboot.platform.finance.model.AdminFinancialSummaryView;
import com.core.coreboot.platform.finance.model.FinancialStoreAggregate;
import com.core.coreboot.platform.finance.service.AdminFinanceService;
import com.core.coreboot.platform.merchant.entity.Store;
import com.core.coreboot.platform.merchant.mapper.StoreMapper;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class AdminFinanceServiceImpl implements AdminFinanceService {
    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_QUERY_DAYS = 366;
    private static final int MAX_ACTION_LENGTH = 64;
    private static final int MAX_KEYWORD_LENGTH = 100;

    private final FinancialReconciliationMapper reconciliationMapper;
    private final AuditLogMapper auditLogMapper;
    private final StoreMapper storeMapper;
    private final SysUserMapper sysUserMapper;
    private final CustomerUserMapper customerUserMapper;
    private final StaffAuthorityMapper staffAuthorityMapper;
    private final Clock clock;

    @Autowired
    public AdminFinanceServiceImpl(
            FinancialReconciliationMapper reconciliationMapper,
            AuditLogMapper auditLogMapper,
            StoreMapper storeMapper,
            SysUserMapper sysUserMapper,
            CustomerUserMapper customerUserMapper,
            StaffAuthorityMapper staffAuthorityMapper
    ) {
        this(
                reconciliationMapper,
                auditLogMapper,
                storeMapper,
                sysUserMapper,
                customerUserMapper,
                staffAuthorityMapper,
                Clock.systemDefaultZone()
        );
    }

    AdminFinanceServiceImpl(
            FinancialReconciliationMapper reconciliationMapper,
            AuditLogMapper auditLogMapper,
            StoreMapper storeMapper,
            SysUserMapper sysUserMapper,
            CustomerUserMapper customerUserMapper,
            StaffAuthorityMapper staffAuthorityMapper,
            Clock clock
    ) {
        this.reconciliationMapper = reconciliationMapper;
        this.auditLogMapper = auditLogMapper;
        this.storeMapper = storeMapper;
        this.sysUserMapper = sysUserMapper;
        this.customerUserMapper = customerUserMapper;
        this.staffAuthorityMapper = staffAuthorityMapper;
        this.clock = clock;
    }

    @Override
    public AdminFinancialReconciliationView reconcile(
            Long operatorId,
            LocalDate startDate,
            LocalDate endDate,
            Long storeId
    ) {
        requireSuperAdmin(operatorId);
        DateRange range = validateDateRange(startDate, endDate);
        Store selectedStore = requireOptionalStore(storeId);
        Map<Long, FinancialStoreAggregate> aggregates = new LinkedHashMap<>();
        mergeRecharge(aggregates, reconciliationMapper.selectRechargeAggregates(
                range.startTime(), range.endTimeExclusive(), storeId));
        mergeRefund(aggregates, reconciliationMapper.selectRefundAggregates(
                range.startTime(), range.endTimeExclusive(), storeId));
        mergeConsumption(aggregates, reconciliationMapper.selectConsumptionAggregates(
                range.startTime(), range.endTimeExclusive(), storeId));
        mergeSettlementPayments(aggregates, reconciliationMapper.selectSettlementPaymentAggregates(
                range.startTime(), range.endTimeExclusive(), storeId));
        if (selectedStore != null) {
            aggregates.computeIfAbsent(selectedStore.getId(), this::emptyAggregate);
        }

        Map<Long, Store> stores = loadStores(aggregates.keySet());
        if (selectedStore != null) {
            stores = new LinkedHashMap<>(stores);
            stores.put(selectedStore.getId(), selectedStore);
        }
        Map<Long, Store> finalStores = stores;
        List<AdminFinancialStoreView> storeViews = aggregates.values().stream()
                .map(aggregate -> toStoreView(aggregate, finalStores.get(aggregate.getStoreId())))
                .sorted(Comparator
                        .comparing(AdminFinancialStoreView::storeName, Comparator.nullsLast(String::compareTo))
                        .thenComparing(AdminFinancialStoreView::storeId))
                .toList();
        return new AdminFinancialReconciliationView(
                startDate,
                endDate,
                LocalDateTime.now(clock),
                summarize(storeViews),
                storeViews
        );
    }

    @Override
    public PageResult<AdminAuditLogView> listAuditLogs(
            Long operatorId,
            int pageNum,
            int pageSize,
            LocalDate startDate,
            LocalDate endDate,
            Long storeId,
            AuditActorType actorType,
            String action,
            String keyword
    ) {
        requireSuperAdmin(operatorId);
        validatePage(pageNum, pageSize);
        DateRange range = validateDateRange(startDate, endDate);
        if (storeId != null && storeId <= 0) {
            throw new CustomException(ExceptionEnum.PLATFORM_INVALID_REQUEST);
        }
        String normalizedAction = normalizeOptional(action);
        String normalizedKeyword = normalizeOptional(keyword);
        if ((normalizedAction != null && normalizedAction.length() > MAX_ACTION_LENGTH)
                || (normalizedKeyword != null && normalizedKeyword.length() > MAX_KEYWORD_LENGTH)) {
            throw new CustomException(ExceptionEnum.PLATFORM_INVALID_REQUEST);
        }

        LambdaQueryWrapper<AuditLog> query = Wrappers.lambdaQuery(AuditLog.class)
                .ge(AuditLog::getCreateTime, range.startTime())
                .lt(AuditLog::getCreateTime, range.endTimeExclusive())
                .eq(storeId != null, AuditLog::getStoreId, storeId)
                .eq(actorType != null, AuditLog::getActorType, actorType)
                .eq(normalizedAction != null, AuditLog::getAction, normalizedAction);
        if (normalizedKeyword != null) {
            query.and(condition -> condition
                    .like(AuditLog::getResourceNo, normalizedKeyword)
                    .or()
                    .like(AuditLog::getRequestId, normalizedKeyword)
                    .or()
                    .like(AuditLog::getRemark, normalizedKeyword));
        }
        query.orderByDesc(AuditLog::getCreateTime).orderByDesc(AuditLog::getId);
        IPage<AuditLog> page = auditLogMapper.selectPage(new Page<>(pageNum, pageSize), query);
        AuditContext context = loadAuditContext(page.getRecords());
        List<AdminAuditLogView> items = page.getRecords().stream()
                .map(log -> toAuditView(log, context))
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
    public List<String> listAuditActions(Long operatorId) {
        requireSuperAdmin(operatorId);
        List<String> actions = auditLogMapper.selectDistinctActions();
        if (actions == null) {
            return List.of();
        }
        return actions.stream()
                .map(this::normalizeOptional)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .toList();
    }

    private void mergeRecharge(
            Map<Long, FinancialStoreAggregate> target,
            List<FinancialStoreAggregate> rows
    ) {
        for (FinancialStoreAggregate row : safeRows(rows)) {
            FinancialStoreAggregate aggregate = targetAggregate(target, row);
            aggregate.setRechargeReceiptCent(nonNegative(row.getRechargeReceiptCent()));
            aggregate.setRechargePoints(nonNegative(row.getRechargePoints()));
        }
    }

    private void mergeRefund(
            Map<Long, FinancialStoreAggregate> target,
            List<FinancialStoreAggregate> rows
    ) {
        for (FinancialStoreAggregate row : safeRows(rows)) {
            FinancialStoreAggregate aggregate = targetAggregate(target, row);
            aggregate.setRefundOutflowCent(nonNegative(row.getRefundOutflowCent()));
            aggregate.setRefundPoints(nonNegative(row.getRefundPoints()));
        }
    }

    private void mergeConsumption(
            Map<Long, FinancialStoreAggregate> target,
            List<FinancialStoreAggregate> rows
    ) {
        for (FinancialStoreAggregate row : safeRows(rows)) {
            FinancialStoreAggregate aggregate = targetAggregate(target, row);
            aggregate.setConsumptionGrossCent(nonNegative(row.getConsumptionGrossCent()));
            aggregate.setConsumptionPoints(nonNegative(row.getConsumptionPoints()));
            aggregate.setPlatformFeeCent(nonNegative(row.getPlatformFeeCent()));
            aggregate.setStorePayableCent(nonNegative(row.getStorePayableCent()));
            aggregate.setNotIncludedPayableCent(nonNegative(row.getNotIncludedPayableCent()));
            aggregate.setIncludedPayableCent(nonNegative(row.getIncludedPayableCent()));
            aggregate.setSettledPayableCent(nonNegative(row.getSettledPayableCent()));
            requireSettlementBreakdown(aggregate);
        }
    }

    private void mergeSettlementPayments(
            Map<Long, FinancialStoreAggregate> target,
            List<FinancialStoreAggregate> rows
    ) {
        for (FinancialStoreAggregate row : safeRows(rows)) {
            FinancialStoreAggregate aggregate = targetAggregate(target, row);
            aggregate.setSettlementPaidCent(nonNegative(row.getSettlementPaidCent()));
        }
    }

    private FinancialStoreAggregate targetAggregate(
            Map<Long, FinancialStoreAggregate> target,
            FinancialStoreAggregate row
    ) {
        if (row == null || row.getStoreId() == null || row.getStoreId() <= 0) {
            throw new CustomException(ExceptionEnum.PLATFORM_DATA_CONFLICT);
        }
        return target.computeIfAbsent(row.getStoreId(), this::emptyAggregate);
    }

    private FinancialStoreAggregate emptyAggregate(Long storeId) {
        FinancialStoreAggregate aggregate = new FinancialStoreAggregate();
        aggregate.setStoreId(storeId);
        aggregate.setRechargeReceiptCent(0L);
        aggregate.setRechargePoints(0L);
        aggregate.setRefundOutflowCent(0L);
        aggregate.setRefundPoints(0L);
        aggregate.setConsumptionGrossCent(0L);
        aggregate.setConsumptionPoints(0L);
        aggregate.setPlatformFeeCent(0L);
        aggregate.setStorePayableCent(0L);
        aggregate.setNotIncludedPayableCent(0L);
        aggregate.setIncludedPayableCent(0L);
        aggregate.setSettledPayableCent(0L);
        aggregate.setSettlementPaidCent(0L);
        return aggregate;
    }

    private AdminFinancialStoreView toStoreView(FinancialStoreAggregate aggregate, Store store) {
        if (store == null) {
            throw new CustomException(ExceptionEnum.PLATFORM_DATA_CONFLICT);
        }
        long receipt = nonNegative(aggregate.getRechargeReceiptCent());
        long refunds = nonNegative(aggregate.getRefundOutflowCent());
        return new AdminFinancialStoreView(
                store.getId(),
                store.getStoreCode(),
                store.getStoreName(),
                store.getStatus(),
                receipt,
                nonNegative(aggregate.getRechargePoints()),
                refunds,
                nonNegative(aggregate.getRefundPoints()),
                subtract(receipt, refunds),
                nonNegative(aggregate.getConsumptionGrossCent()),
                nonNegative(aggregate.getConsumptionPoints()),
                nonNegative(aggregate.getPlatformFeeCent()),
                nonNegative(aggregate.getStorePayableCent()),
                nonNegative(aggregate.getNotIncludedPayableCent()),
                nonNegative(aggregate.getIncludedPayableCent()),
                nonNegative(aggregate.getSettledPayableCent()),
                nonNegative(aggregate.getSettlementPaidCent())
        );
    }

    private AdminFinancialSummaryView summarize(List<AdminFinancialStoreView> stores) {
        return new AdminFinancialSummaryView(
                sum(stores, AdminFinancialStoreView::rechargeReceiptCent),
                sum(stores, AdminFinancialStoreView::rechargePoints),
                sum(stores, AdminFinancialStoreView::refundOutflowCent),
                sum(stores, AdminFinancialStoreView::refundPoints),
                sum(stores, AdminFinancialStoreView::netRechargeCashCent),
                sum(stores, AdminFinancialStoreView::consumptionGrossCent),
                sum(stores, AdminFinancialStoreView::consumptionPoints),
                sum(stores, AdminFinancialStoreView::platformFeeCent),
                sum(stores, AdminFinancialStoreView::storePayableCent),
                sum(stores, AdminFinancialStoreView::notIncludedPayableCent),
                sum(stores, AdminFinancialStoreView::includedPayableCent),
                sum(stores, AdminFinancialStoreView::settledPayableCent),
                sum(stores, AdminFinancialStoreView::settlementPaidCent)
        );
    }

    private long sum(
            List<AdminFinancialStoreView> stores,
            java.util.function.ToLongFunction<AdminFinancialStoreView> getter
    ) {
        long total = 0;
        try {
            for (AdminFinancialStoreView store : stores) {
                total = Math.addExact(total, getter.applyAsLong(store));
            }
            return total;
        } catch (ArithmeticException ex) {
            throw new CustomException(ExceptionEnum.PLATFORM_DATA_CONFLICT);
        }
    }

    private long subtract(long left, long right) {
        try {
            return Math.subtractExact(left, right);
        } catch (ArithmeticException ex) {
            throw new CustomException(ExceptionEnum.PLATFORM_DATA_CONFLICT);
        }
    }

    private void requireSettlementBreakdown(FinancialStoreAggregate aggregate) {
        try {
            long breakdown = Math.addExact(
                    Math.addExact(
                            aggregate.getNotIncludedPayableCent(),
                            aggregate.getIncludedPayableCent()
                    ),
                    aggregate.getSettledPayableCent()
            );
            if (breakdown != aggregate.getStorePayableCent()) {
                throw new CustomException(ExceptionEnum.PLATFORM_DATA_CONFLICT);
            }
        } catch (ArithmeticException ex) {
            throw new CustomException(ExceptionEnum.PLATFORM_DATA_CONFLICT);
        }
    }

    private AuditContext loadAuditContext(List<AuditLog> logs) {
        List<Long> sysUserIds = logs.stream()
                .filter(log -> log.getActorType() == AuditActorType.SYS_USER)
                .map(AuditLog::getActorId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        List<Long> customerIds = logs.stream()
                .filter(log -> log.getActorType() == AuditActorType.CUSTOMER)
                .map(AuditLog::getActorId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        List<Long> storeIds = logs.stream()
                .map(AuditLog::getStoreId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        return new AuditContext(
                loadSysUsers(sysUserIds),
                loadCustomers(customerIds),
                loadStores(storeIds)
        );
    }

    private AdminAuditLogView toAuditView(AuditLog log, AuditContext context) {
        Store store = context.stores().get(log.getStoreId());
        return new AdminAuditLogView(
                log.getId(),
                log.getActorType(),
                log.getActorId(),
                actorName(log, context),
                log.getOperatorRole(),
                log.getStoreId(),
                store == null ? null : store.getStoreCode(),
                store == null ? null : store.getStoreName(),
                log.getAction(),
                log.getResourceType(),
                log.getResourceNo(),
                log.getRequestId(),
                log.getBeforeSnapshot(),
                log.getAfterSnapshot(),
                log.getRemark(),
                log.getClientIp(),
                log.getCreateTime()
        );
    }

    private String actorName(AuditLog log, AuditContext context) {
        if (log.getActorType() == AuditActorType.SYSTEM) {
            return "系统";
        }
        if (log.getActorType() == AuditActorType.SYS_USER) {
            SysUser user = context.sysUsers().get(log.getActorId());
            if (user == null) {
                return log.getActorId() == null ? "后台操作员" : "后台操作员 " + log.getActorId();
            }
            String realName = normalizeOptional(user.getRealName());
            return realName == null ? user.getUsername() : realName;
        }
        if (log.getActorType() == AuditActorType.CUSTOMER) {
            CustomerUser customer = context.customers().get(log.getActorId());
            if (customer == null) {
                return log.getActorId() == null ? "用户" : "用户 " + log.getActorId();
            }
            String nickname = normalizeOptional(customer.getNickname());
            if (nickname != null) {
                return nickname;
            }
            String phone = normalizeOptional(customer.getPhone());
            return phone == null ? "用户 " + customer.getId() : phone;
        }
        return "未知主体";
    }

    private Map<Long, Store> loadStores(Collection<Long> ids) {
        List<Long> validIds = validIds(ids);
        if (validIds.isEmpty()) {
            return Map.of();
        }
        return storeMapper.selectByIds(validIds).stream()
                .collect(Collectors.toMap(Store::getId, Function.identity()));
    }

    private Map<Long, SysUser> loadSysUsers(Collection<Long> ids) {
        List<Long> validIds = validIds(ids);
        if (validIds.isEmpty()) {
            return Map.of();
        }
        return sysUserMapper.selectByIds(validIds).stream()
                .collect(Collectors.toMap(SysUser::getId, Function.identity()));
    }

    private Map<Long, CustomerUser> loadCustomers(Collection<Long> ids) {
        List<Long> validIds = validIds(ids);
        if (validIds.isEmpty()) {
            return Map.of();
        }
        return customerUserMapper.selectByIds(validIds).stream()
                .collect(Collectors.toMap(CustomerUser::getId, Function.identity()));
    }

    private List<Long> validIds(Collection<Long> ids) {
        if (ids == null) {
            return List.of();
        }
        return ids.stream()
                .filter(Objects::nonNull)
                .filter(id -> id > 0)
                .distinct()
                .toList();
    }

    private List<FinancialStoreAggregate> safeRows(List<FinancialStoreAggregate> rows) {
        return rows == null ? List.of() : rows;
    }

    private long nonNegative(Long value) {
        if (value == null) {
            return 0L;
        }
        if (value < 0) {
            throw new CustomException(ExceptionEnum.PLATFORM_DATA_CONFLICT);
        }
        return value;
    }

    private Store requireOptionalStore(Long storeId) {
        if (storeId == null) {
            return null;
        }
        if (storeId <= 0) {
            throw new CustomException(ExceptionEnum.PLATFORM_INVALID_REQUEST);
        }
        Store store = storeMapper.selectById(storeId);
        if (store == null) {
            throw new CustomException(ExceptionEnum.PLATFORM_STORE_NOT_FOUND);
        }
        return store;
    }

    private DateRange validateDateRange(LocalDate startDate, LocalDate endDate) {
        LocalDate today = LocalDate.now(clock);
        if (startDate == null || endDate == null
                || endDate.isBefore(startDate)
                || endDate.isAfter(today)
                || ChronoUnit.DAYS.between(startDate, endDate) >= MAX_QUERY_DAYS) {
            throw new CustomException(ExceptionEnum.PLATFORM_INVALID_REQUEST);
        }
        return new DateRange(startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay());
    }

    private void validatePage(int pageNum, int pageSize) {
        if (pageNum < 1 || pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw new CustomException(ExceptionEnum.PLATFORM_INVALID_REQUEST);
        }
    }

    private void requireSuperAdmin(Long operatorId) {
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
        if (roles == null || !roles.contains(RoleCode.SUPER_ADMIN.getCode())) {
            throw new CustomException(ExceptionEnum.PLATFORM_ADMIN_ACCESS_DENIED);
        }
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private record DateRange(LocalDateTime startTime, LocalDateTime endTimeExclusive) {
    }

    private record AuditContext(
            Map<Long, SysUser> sysUsers,
            Map<Long, CustomerUser> customers,
            Map<Long, Store> stores
    ) {
    }
}
