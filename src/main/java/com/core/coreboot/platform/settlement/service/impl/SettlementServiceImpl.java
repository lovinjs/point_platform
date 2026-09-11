package com.core.coreboot.platform.settlement.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.core.coreboot.exception.CustomException;
import com.core.coreboot.exception.ExceptionEnum;
import com.core.coreboot.platform.audit.entity.AuditLog;
import com.core.coreboot.platform.audit.mapper.AuditLogMapper;
import com.core.coreboot.platform.common.enums.AuditActorType;
import com.core.coreboot.platform.common.enums.ConsumptionOrderStatus;
import com.core.coreboot.platform.common.enums.RoleCode;
import com.core.coreboot.platform.common.enums.SettlementItemType;
import com.core.coreboot.platform.common.enums.SettlementPeriodStatus;
import com.core.coreboot.platform.common.enums.SettlementStatus;
import com.core.coreboot.platform.common.enums.StoreSettlementStatus;
import com.core.coreboot.platform.common.enums.SysUserStatus;
import com.core.coreboot.platform.common.model.PageResult;
import com.core.coreboot.platform.common.support.BusinessNoGenerator;
import com.core.coreboot.platform.consumption.entity.ConsumptionOrder;
import com.core.coreboot.platform.consumption.mapper.ConsumptionOrderMapper;
import com.core.coreboot.platform.merchant.entity.Store;
import com.core.coreboot.platform.merchant.mapper.StoreMapper;
import com.core.coreboot.platform.settlement.entity.SettlementPeriod;
import com.core.coreboot.platform.settlement.entity.StoreSettlement;
import com.core.coreboot.platform.settlement.entity.StoreSettlementItem;
import com.core.coreboot.platform.settlement.mapper.SettlementPeriodMapper;
import com.core.coreboot.platform.settlement.mapper.StoreSettlementItemMapper;
import com.core.coreboot.platform.settlement.mapper.StoreSettlementMapper;
import com.core.coreboot.platform.settlement.model.SettlementConfirmCommand;
import com.core.coreboot.platform.settlement.model.SettlementGenerateCommand;
import com.core.coreboot.platform.settlement.model.SettlementGenerationResult;
import com.core.coreboot.platform.settlement.model.SettlementPaymentCommand;
import com.core.coreboot.platform.settlement.model.StoreSettlementDetailView;
import com.core.coreboot.platform.settlement.model.StoreSettlementItemView;
import com.core.coreboot.platform.settlement.model.StoreSettlementSummaryView;
import com.core.coreboot.platform.settlement.service.SettlementService;
import com.core.coreboot.platform.staff.entity.SysUser;
import com.core.coreboot.platform.staff.mapper.StaffAuthorityMapper;
import com.core.coreboot.platform.staff.mapper.SysUserMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class SettlementServiceImpl implements SettlementService {
    private static final int MAX_PAGE_SIZE = 50;
    private static final int SETTLEMENT_NO_MAX_LENGTH = 64;
    private static final int PAYMENT_REFERENCE_MAX_LENGTH = 128;
    private static final int REMARK_MAX_LENGTH = 500;
    private static final DateTimeFormatter PERIOD_FORMATTER = new java.time.format.DateTimeFormatterBuilder()
            .appendPattern("uuuu-MM")
            .toFormatter()
            .withResolverStyle(ResolverStyle.STRICT);

    private final SettlementPeriodMapper settlementPeriodMapper;
    private final StoreSettlementMapper storeSettlementMapper;
    private final StoreSettlementItemMapper storeSettlementItemMapper;
    private final ConsumptionOrderMapper consumptionOrderMapper;
    private final StoreMapper storeMapper;
    private final SysUserMapper sysUserMapper;
    private final StaffAuthorityMapper staffAuthorityMapper;
    private final AuditLogMapper auditLogMapper;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Autowired
    public SettlementServiceImpl(
            SettlementPeriodMapper settlementPeriodMapper,
            StoreSettlementMapper storeSettlementMapper,
            StoreSettlementItemMapper storeSettlementItemMapper,
            ConsumptionOrderMapper consumptionOrderMapper,
            StoreMapper storeMapper,
            SysUserMapper sysUserMapper,
            StaffAuthorityMapper staffAuthorityMapper,
            AuditLogMapper auditLogMapper,
            ObjectMapper objectMapper
    ) {
        this(
                settlementPeriodMapper,
                storeSettlementMapper,
                storeSettlementItemMapper,
                consumptionOrderMapper,
                storeMapper,
                sysUserMapper,
                staffAuthorityMapper,
                auditLogMapper,
                objectMapper,
                Clock.systemDefaultZone()
        );
    }

    SettlementServiceImpl(
            SettlementPeriodMapper settlementPeriodMapper,
            StoreSettlementMapper storeSettlementMapper,
            StoreSettlementItemMapper storeSettlementItemMapper,
            ConsumptionOrderMapper consumptionOrderMapper,
            StoreMapper storeMapper,
            SysUserMapper sysUserMapper,
            StaffAuthorityMapper staffAuthorityMapper,
            AuditLogMapper auditLogMapper,
            ObjectMapper objectMapper,
            Clock clock
    ) {
        this.settlementPeriodMapper = settlementPeriodMapper;
        this.storeSettlementMapper = storeSettlementMapper;
        this.storeSettlementItemMapper = storeSettlementItemMapper;
        this.consumptionOrderMapper = consumptionOrderMapper;
        this.storeMapper = storeMapper;
        this.sysUserMapper = sysUserMapper;
        this.staffAuthorityMapper = staffAuthorityMapper;
        this.auditLogMapper = auditLogMapper;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SettlementGenerationResult generate(SettlementGenerateCommand command) {
        if (command == null) {
            throw new CustomException(ExceptionEnum.PLATFORM_INVALID_REQUEST);
        }
        Authority authority = resolveAuthority(command.operatorId());
        requireSuperAdmin(authority);
        YearMonth targetMonth = parsePeriodCode(command.periodCode());
        if (!targetMonth.isBefore(YearMonth.from(LocalDate.now(clock)))) {
            throw new CustomException(ExceptionEnum.PLATFORM_SETTLEMENT_PERIOD_NOT_CLOSED);
        }
        String clientIp = normalizeClientIp(command.clientIp());
        LocalDate startDate = targetMonth.atDay(1);
        LocalDate endDate = targetMonth.atEndOfMonth();

        settlementPeriodMapper.insertOpenPeriod(targetMonth.toString(), startDate, endDate);
        SettlementPeriod period = settlementPeriodMapper.selectByPeriodCodeForUpdate(targetMonth.toString());
        validatePeriod(period, startDate, endDate);
        if (period.getPeriodStatus() != SettlementPeriodStatus.OPEN) {
            List<StoreSettlement> generatedSettlements =
                    storeSettlementMapper.selectByPeriodId(period.getId());
            if (period.getPeriodStatus() == SettlementPeriodStatus.FROZEN
                    || period.getGeneratedTime() == null
                    || generatedSettlements == null
                    || generatedSettlements.isEmpty()) {
                throw new CustomException(ExceptionEnum.PLATFORM_SETTLEMENT_DATA_INVALID);
            }
            return toGenerationResult(period, generatedSettlements);
        }
        List<StoreSettlement> unexpected = storeSettlementMapper.selectByPeriodId(period.getId());
        if (unexpected != null && !unexpected.isEmpty()) {
            throw new CustomException(ExceptionEnum.PLATFORM_SETTLEMENT_DATA_INVALID);
        }

        LocalDateTime endTimeExclusive = targetMonth.plusMonths(1).atDay(1).atStartOfDay();
        List<ConsumptionOrder> eligibleOrders = consumptionOrderMapper.selectEligibleForSettlement(
                startDate.atStartOfDay(),
                endTimeExclusive
        );
        List<ConsumptionOrder> eligibleReversals =
                consumptionOrderMapper.selectEligibleReversalsForSettlement(endTimeExclusive);
        eligibleOrders = eligibleOrders == null ? List.of() : eligibleOrders;
        eligibleReversals = eligibleReversals == null ? List.of() : eligibleReversals;
        if (eligibleOrders.isEmpty() && eligibleReversals.isEmpty()) {
            throw new CustomException(ExceptionEnum.PLATFORM_SETTLEMENT_NO_ELIGIBLE_ORDERS);
        }

        Map<Long, SettlementBatch> batchesByStore = new LinkedHashMap<>();
        for (ConsumptionOrder order : eligibleOrders) {
            validateEligibleOrder(order);
            batchesByStore.computeIfAbsent(order.getStoreId(), ignored -> new SettlementBatch(
                    new ArrayList<>(),
                    new ArrayList<>()
            )).orders().add(order);
        }
        for (ConsumptionOrder reversal : eligibleReversals) {
            validateEligibleReversal(reversal);
            batchesByStore.computeIfAbsent(reversal.getStoreId(), ignored -> new SettlementBatch(
                    new ArrayList<>(),
                    new ArrayList<>()
            )).reversals().add(reversal);
        }
        for (Map.Entry<Long, SettlementBatch> entry : batchesByStore.entrySet()) {
            createStoreSettlement(period, entry.getKey(), entry.getValue());
        }

        LocalDateTime generatedTime = LocalDateTime.now(clock);
        requireOneRow(settlementPeriodMapper.markGenerated(period.getId(), generatedTime));
        period.setPeriodStatus(SettlementPeriodStatus.GENERATED);
        period.setFrozenTime(generatedTime);
        period.setGeneratedTime(generatedTime);
        List<StoreSettlement> settlements = storeSettlementMapper.selectByPeriodId(period.getId());
        if (settlements == null || settlements.size() != batchesByStore.size()) {
            throw new CustomException(ExceptionEnum.PLATFORM_SETTLEMENT_DATA_INVALID);
        }
        requireOneRow(auditLogMapper.insert(buildGenerationAudit(
                period,
                command.operatorId(),
                clientIp,
                eligibleOrders.size() + eligibleReversals.size(),
                settlements
        )));
        return toGenerationResult(period, settlements);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<StoreSettlementSummaryView> list(
            Long operatorId,
            int pageNum,
            int pageSize,
            String periodCode,
            StoreSettlementStatus status,
            Long storeId
    ) {
        validatePage(pageNum, pageSize);
        Authority authority = resolveAuthority(operatorId);
        requireSettlementReader(authority);
        if (storeId != null && storeId <= 0) {
            throw new CustomException(ExceptionEnum.PLATFORM_INVALID_REQUEST);
        }
        if (storeId != null && !authority.superAdmin() && !authority.storeIds().contains(storeId)) {
            throw new CustomException(ExceptionEnum.PLATFORM_SETTLEMENT_ACCESS_DENIED);
        }

        var query = Wrappers.lambdaQuery(StoreSettlement.class);
        String normalizedPeriodCode = normalizeOptional(periodCode);
        if (normalizedPeriodCode != null) {
            parsePeriodCode(normalizedPeriodCode);
            SettlementPeriod period = settlementPeriodMapper.selectByPeriodCode(normalizedPeriodCode);
            if (period == null) {
                return emptyPage(pageNum, pageSize);
            }
            query.eq(StoreSettlement::getPeriodId, period.getId());
        }
        if (status != null) {
            query.eq(StoreSettlement::getSettlementStatus, status);
        }
        if (storeId != null) {
            query.eq(StoreSettlement::getStoreId, storeId);
        } else if (!authority.superAdmin()) {
            if (authority.storeIds().isEmpty()) {
                return emptyPage(pageNum, pageSize);
            }
            query.in(StoreSettlement::getStoreId, authority.storeIds());
        }
        query.orderByDesc(StoreSettlement::getPeriodId)
                .orderByAsc(StoreSettlement::getStoreId)
                .orderByDesc(StoreSettlement::getId);

        IPage<StoreSettlement> page = storeSettlementMapper.selectPage(
                new Page<>(pageNum, pageSize),
                query
        );
        List<StoreSettlementSummaryView> items = toSummaryViews(page.getRecords());
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
    @Transactional(readOnly = true)
    public StoreSettlementDetailView detail(Long operatorId, String settlementNo) {
        Authority authority = resolveAuthority(operatorId);
        requireSettlementReader(authority);
        StoreSettlement settlement = requireSettlement(settlementNo, false);
        requireReadAccess(authority, settlement.getStoreId());
        return toDetailView(settlement);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StoreSettlementDetailView confirm(SettlementConfirmCommand command) {
        NormalizedConfirm normalized = normalizeConfirm(command);
        Authority authority = resolveAuthority(normalized.operatorId());
        StoreSettlement settlement = requireSettlement(normalized.settlementNo(), true);
        requireManagerAccess(authority, settlement.getStoreId());

        StoreSettlementStatus status = settlement.getSettlementStatus();
        if (status == StoreSettlementStatus.CONFIRMED
                || status == StoreSettlementStatus.PAID
                || status == StoreSettlementStatus.CLOSED) {
            return toDetailView(settlement);
        }
        if (status != StoreSettlementStatus.GENERATED) {
            throw new CustomException(ExceptionEnum.PLATFORM_SETTLEMENT_STATE_INVALID);
        }

        LocalDateTime confirmedTime = LocalDateTime.now(clock);
        requireOneRow(storeSettlementMapper.markConfirmed(
                settlement.getId(),
                confirmedTime,
                normalized.remark()
        ));
        settlementPeriodMapper.markConfirmedIfAll(settlement.getPeriodId());
        requireOneRow(auditLogMapper.insert(buildSettlementAudit(
                "STORE_SETTLEMENT_CONFIRMED",
                settlement,
                StoreSettlementStatus.GENERATED,
                StoreSettlementStatus.CONFIRMED,
                normalized.operatorId(),
                RoleCode.STORE_MANAGER,
                normalized.clientIp(),
                normalized.remark(),
                null,
                confirmedTime
        )));
        return toDetailView(requireSettlement(normalized.settlementNo(), false));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StoreSettlementDetailView markPaid(SettlementPaymentCommand command) {
        NormalizedPayment normalized = normalizePayment(command);
        Authority authority = resolveAuthority(normalized.operatorId());
        requireSuperAdmin(authority);
        StoreSettlement settlement = requireSettlement(normalized.settlementNo(), true);
        StoreSettlementStatus status = settlement.getSettlementStatus();
        if (status == StoreSettlementStatus.PAID || status == StoreSettlementStatus.CLOSED) {
            if (Objects.equals(settlement.getPaymentReference(), normalized.paymentReference())) {
                return toDetailView(settlement);
            }
            throw new CustomException(ExceptionEnum.PLATFORM_SETTLEMENT_STATE_INVALID);
        }
        if (status != StoreSettlementStatus.CONFIRMED) {
            throw new CustomException(ExceptionEnum.PLATFORM_SETTLEMENT_STATE_INVALID);
        }
        if (storeSettlementMapper.countByPaymentReferenceExcluding(
                normalized.paymentReference(),
                settlement.getId()
        ) > 0) {
            throw new CustomException(ExceptionEnum.PLATFORM_SETTLEMENT_PAYMENT_REFERENCE_USED);
        }
        long totalItemCount = storeSettlementItemMapper.selectCount(
                Wrappers.lambdaQuery(StoreSettlementItem.class)
                        .eq(StoreSettlementItem::getSettlementId, settlement.getId())
        );
        long expectedConsumptionCount = storeSettlementItemMapper.selectCount(
                Wrappers.lambdaQuery(StoreSettlementItem.class)
                        .eq(StoreSettlementItem::getSettlementId, settlement.getId())
                        .eq(StoreSettlementItem::getItemType, SettlementItemType.CONSUMPTION)
        );
        if (totalItemCount <= 0 || totalItemCount > Integer.MAX_VALUE
                || expectedConsumptionCount < 0 || expectedConsumptionCount > Integer.MAX_VALUE) {
            throw new CustomException(ExceptionEnum.PLATFORM_SETTLEMENT_DATA_INVALID);
        }

        LocalDateTime paidTime = LocalDateTime.now(clock);
        requireOneRow(storeSettlementMapper.markPaid(
                settlement.getId(),
                paidTime,
                normalized.paymentReference(),
                normalized.remark()
        ));
        if (expectedConsumptionCount > 0) {
            int settledOrders = consumptionOrderMapper.markSettlementItemsSettled(settlement.getId());
            if (settledOrders != (int) expectedConsumptionCount) {
                throw new CustomException(ExceptionEnum.PLATFORM_SETTLEMENT_WRITE_FAILED);
            }
        }
        settlementPeriodMapper.markPaidIfAll(settlement.getPeriodId());
        requireOneRow(auditLogMapper.insert(buildSettlementAudit(
                "STORE_SETTLEMENT_PAID",
                settlement,
                StoreSettlementStatus.CONFIRMED,
                StoreSettlementStatus.PAID,
                normalized.operatorId(),
                RoleCode.SUPER_ADMIN,
                normalized.clientIp(),
                normalized.remark(),
                normalized.paymentReference(),
                paidTime
        )));
        return toDetailView(requireSettlement(normalized.settlementNo(), false));
    }

    private void createStoreSettlement(
            SettlementPeriod period,
            Long storeId,
            SettlementBatch batch
    ) {
        Store store = storeMapper.selectById(storeId);
        if (store == null || store.getMerchantId() == null || store.getMerchantId() <= 0) {
            throw new CustomException(ExceptionEnum.PLATFORM_SETTLEMENT_DATA_INVALID);
        }
        Totals totals = calculateTotals(batch);
        StoreSettlement settlement = StoreSettlement.builder()
                .settlementNo(BusinessNoGenerator.next("STL"))
                .periodId(period.getId())
                .merchantId(store.getMerchantId())
                .storeId(storeId)
                .totalConsumePoints(totals.points())
                .grossAmountCent(totals.grossAmountCent())
                .platformFeeCent(totals.platformFeeCent())
                .adjustmentAmountCent(0L)
                .payableAmountCent(totals.storePayableCent())
                .settlementStatus(StoreSettlementStatus.GENERATED)
                .build();
        requireOneRow(storeSettlementMapper.insert(settlement));
        if (settlement.getId() == null) {
            throw new CustomException(ExceptionEnum.PLATFORM_SETTLEMENT_WRITE_FAILED);
        }
        for (ConsumptionOrder order : batch.orders()) {
            StoreSettlementItem item = StoreSettlementItem.builder()
                    .settlementId(settlement.getId())
                    .itemType(SettlementItemType.CONSUMPTION)
                    .consumptionOrderId(order.getId())
                    .pointsDelta(order.getConsumePoints())
                    .grossAmountCent(order.getGrossAmountCent())
                    .platformFeeCent(order.getPlatformFeeCent())
                    .storePayableCent(order.getStorePayableCent())
                    .build();
            requireOneRow(storeSettlementItemMapper.insert(item));
            requireOneRow(consumptionOrderMapper.markIncludedInSettlement(order.getId()));
        }
        for (ConsumptionOrder reversal : batch.reversals()) {
            StoreSettlementItem item = StoreSettlementItem.builder()
                    .settlementId(settlement.getId())
                    .itemType(SettlementItemType.REVERSAL_ADJUSTMENT)
                    .consumptionOrderId(reversal.getId())
                    .pointsDelta(negateExact(reversal.getConsumePoints()))
                    .grossAmountCent(negateExact(reversal.getGrossAmountCent()))
                    .platformFeeCent(negateExact(reversal.getPlatformFeeCent()))
                    .storePayableCent(negateExact(reversal.getStorePayableCent()))
                    .adjustmentReason(reversal.getReversalReason())
                    .build();
            requireOneRow(storeSettlementItemMapper.insert(item));
            requireOneRow(consumptionOrderMapper.markReversalAdjusted(reversal.getId()));
        }
    }

    private Totals calculateTotals(SettlementBatch batch) {
        long points = 0;
        long gross = 0;
        long fee = 0;
        long payable = 0;
        try {
            for (ConsumptionOrder order : batch.orders()) {
                points = Math.addExact(points, order.getConsumePoints());
                gross = Math.addExact(gross, order.getGrossAmountCent());
                fee = Math.addExact(fee, order.getPlatformFeeCent());
                payable = Math.addExact(payable, order.getStorePayableCent());
            }
            for (ConsumptionOrder reversal : batch.reversals()) {
                points = Math.subtractExact(points, reversal.getConsumePoints());
                gross = Math.subtractExact(gross, reversal.getGrossAmountCent());
                fee = Math.subtractExact(fee, reversal.getPlatformFeeCent());
                payable = Math.subtractExact(payable, reversal.getStorePayableCent());
            }
        } catch (ArithmeticException ex) {
            throw new CustomException(ExceptionEnum.PLATFORM_SETTLEMENT_DATA_INVALID);
        }
        try {
            if (gross != Math.addExact(fee, payable)) {
                throw new CustomException(ExceptionEnum.PLATFORM_SETTLEMENT_DATA_INVALID);
            }
        } catch (ArithmeticException ex) {
            throw new CustomException(ExceptionEnum.PLATFORM_SETTLEMENT_DATA_INVALID);
        }
        return new Totals(points, gross, fee, payable);
    }

    private long negateExact(Long value) {
        if (value == null) {
            throw new CustomException(ExceptionEnum.PLATFORM_SETTLEMENT_DATA_INVALID);
        }
        try {
            return Math.negateExact(value);
        } catch (ArithmeticException ex) {
            throw new CustomException(ExceptionEnum.PLATFORM_SETTLEMENT_DATA_INVALID);
        }
    }

    private void validateEligibleOrder(ConsumptionOrder order) {
        if (order == null || order.getId() == null || order.getStoreId() == null
                || order.getStoreId() <= 0 || order.getConsumePoints() == null
                || order.getConsumePoints() <= 0 || order.getGrossAmountCent() == null
                || order.getGrossAmountCent() <= 0 || order.getPlatformFeeCent() == null
                || order.getPlatformFeeCent() < 0 || order.getStorePayableCent() == null
                || order.getStorePayableCent() < 0
                || order.getOrderStatus() != ConsumptionOrderStatus.COMPLETED
                || order.getSettlementStatus() != SettlementStatus.NOT_INCLUDED
                || order.getCompletedTime() == null) {
            throw new CustomException(ExceptionEnum.PLATFORM_SETTLEMENT_DATA_INVALID);
        }
        try {
            long expectedGross = Math.multiplyExact(order.getConsumePoints(), 100L);
            long splitTotal = Math.addExact(order.getPlatformFeeCent(), order.getStorePayableCent());
            if (expectedGross != order.getGrossAmountCent() || splitTotal != order.getGrossAmountCent()) {
                throw new CustomException(ExceptionEnum.PLATFORM_SETTLEMENT_DATA_INVALID);
            }
        } catch (ArithmeticException ex) {
            throw new CustomException(ExceptionEnum.PLATFORM_SETTLEMENT_DATA_INVALID);
        }
    }

    private void validateEligibleReversal(ConsumptionOrder order) {
        if (order == null || order.getId() == null || order.getStoreId() == null
                || order.getStoreId() <= 0 || order.getConsumePoints() == null
                || order.getConsumePoints() <= 0 || order.getGrossAmountCent() == null
                || order.getGrossAmountCent() <= 0 || order.getPlatformFeeCent() == null
                || order.getPlatformFeeCent() < 0 || order.getStorePayableCent() == null
                || order.getStorePayableCent() < 0
                || order.getOrderStatus() != ConsumptionOrderStatus.REVERSED
                || order.getSettlementStatus() != SettlementStatus.SETTLED
                || order.getCompletedTime() == null || order.getReversedTime() == null
                || normalizeOptional(order.getReversalReason()) == null) {
            throw new CustomException(ExceptionEnum.PLATFORM_SETTLEMENT_DATA_INVALID);
        }
        try {
            long expectedGross = Math.multiplyExact(order.getConsumePoints(), 100L);
            long splitTotal = Math.addExact(order.getPlatformFeeCent(), order.getStorePayableCent());
            if (expectedGross != order.getGrossAmountCent() || splitTotal != order.getGrossAmountCent()) {
                throw new CustomException(ExceptionEnum.PLATFORM_SETTLEMENT_DATA_INVALID);
            }
        } catch (ArithmeticException ex) {
            throw new CustomException(ExceptionEnum.PLATFORM_SETTLEMENT_DATA_INVALID);
        }
    }

    private SettlementGenerationResult toGenerationResult(
            SettlementPeriod period,
            List<StoreSettlement> settlements
    ) {
        List<StoreSettlementSummaryView> views = toSummaryViews(settlements);
        return new SettlementGenerationResult(
                period.getPeriodCode(),
                period.getStartDate(),
                period.getEndDate(),
                period.getPeriodStatus(),
                period.getGeneratedTime(),
                views.size(),
                views
        );
    }

    private StoreSettlementDetailView toDetailView(StoreSettlement settlement) {
        StoreSettlementSummaryView summary = toSummaryViews(List.of(settlement)).getFirst();
        List<StoreSettlementItem> items = storeSettlementItemMapper.selectList(
                Wrappers.lambdaQuery(StoreSettlementItem.class)
                        .eq(StoreSettlementItem::getSettlementId, settlement.getId())
                        .orderByAsc(StoreSettlementItem::getId)
        );
        Map<Long, ConsumptionOrder> consumptionOrders = loadConsumptionOrders(
                items.stream().map(StoreSettlementItem::getConsumptionOrderId).toList()
        );
        List<StoreSettlementItemView> itemViews = items.stream().map(item -> {
            ConsumptionOrder order = consumptionOrders.get(item.getConsumptionOrderId());
            if (item.getItemType() == null
                    || (item.getItemType() == SettlementItemType.CONSUMPTION && order == null)) {
                throw new CustomException(ExceptionEnum.PLATFORM_SETTLEMENT_DATA_INVALID);
            }
            return new StoreSettlementItemView(
                    item.getId(),
                    item.getItemType(),
                    order == null ? null : order.getOrderNo(),
                    requireValue(item.getPointsDelta()),
                    requireValue(item.getGrossAmountCent()),
                    requireValue(item.getPlatformFeeCent()),
                    requireValue(item.getStorePayableCent()),
                    item.getAdjustmentReason(),
                    order == null ? null : order.getCompletedTime(),
                    item.getCreateTime()
            );
        }).toList();
        return new StoreSettlementDetailView(summary, itemViews);
    }

    private List<StoreSettlementSummaryView> toSummaryViews(List<StoreSettlement> settlements) {
        if (settlements == null || settlements.isEmpty()) {
            return List.of();
        }
        if (settlements.stream().anyMatch(item -> item.getPeriodId() == null || item.getStoreId() == null)) {
            throw new CustomException(ExceptionEnum.PLATFORM_SETTLEMENT_DATA_INVALID);
        }
        Set<Long> periodIds = distinctIds(settlements.stream().map(StoreSettlement::getPeriodId).toList());
        Set<Long> storeIds = distinctIds(settlements.stream().map(StoreSettlement::getStoreId).toList());
        Map<Long, SettlementPeriod> periods = settlementPeriodMapper.selectByIds(periodIds).stream()
                .collect(Collectors.toMap(SettlementPeriod::getId, Function.identity()));
        Map<Long, Store> stores = storeMapper.selectByIds(storeIds).stream()
                .collect(Collectors.toMap(Store::getId, Function.identity()));
        return settlements.stream().map(settlement -> {
            SettlementPeriod period = periods.get(settlement.getPeriodId());
            Store store = stores.get(settlement.getStoreId());
            if (period == null || store == null || settlement.getSettlementStatus() == null) {
                throw new CustomException(ExceptionEnum.PLATFORM_SETTLEMENT_DATA_INVALID);
            }
            return new StoreSettlementSummaryView(
                    settlement.getSettlementNo(),
                    period.getPeriodCode(),
                    period.getStartDate(),
                    period.getEndDate(),
                    settlement.getMerchantId(),
                    settlement.getStoreId(),
                    store.getStoreName(),
                    requireValue(settlement.getTotalConsumePoints()),
                    requireValue(settlement.getGrossAmountCent()),
                    requireValue(settlement.getPlatformFeeCent()),
                    requireValue(settlement.getAdjustmentAmountCent()),
                    requireValue(settlement.getPayableAmountCent()),
                    settlement.getSettlementStatus(),
                    settlement.getConfirmedTime(),
                    settlement.getPaidTime(),
                    settlement.getPaymentReference(),
                    settlement.getRemark(),
                    settlement.getCreateTime(),
                    settlement.getUpdateTime()
            );
        }).toList();
    }

    private Map<Long, ConsumptionOrder> loadConsumptionOrders(Collection<Long> orderIds) {
        Set<Long> ids = distinctIds(orderIds);
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return consumptionOrderMapper.selectByIds(ids).stream()
                .collect(Collectors.toMap(ConsumptionOrder::getId, Function.identity()));
    }

    private Set<Long> distinctIds(Collection<Long> ids) {
        if (ids == null) {
            return Set.of();
        }
        return ids.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private StoreSettlement requireSettlement(String settlementNo, boolean forUpdate) {
        String normalized = normalizeRequired(settlementNo);
        if (normalized == null || normalized.length() > SETTLEMENT_NO_MAX_LENGTH) {
            throw new CustomException(ExceptionEnum.PLATFORM_INVALID_REQUEST);
        }
        StoreSettlement settlement = forUpdate
                ? storeSettlementMapper.selectBySettlementNoForUpdate(normalized)
                : storeSettlementMapper.selectBySettlementNo(normalized);
        if (settlement == null) {
            throw new CustomException(ExceptionEnum.PLATFORM_SETTLEMENT_NOT_FOUND);
        }
        return settlement;
    }

    private Authority resolveAuthority(Long operatorId) {
        if (operatorId == null || operatorId <= 0) {
            throw new CustomException(ExceptionEnum.PLATFORM_OPERATOR_NOT_FOUND);
        }
        SysUser operator = sysUserMapper.selectById(operatorId);
        if (operator == null) {
            throw new CustomException(ExceptionEnum.PLATFORM_OPERATOR_NOT_FOUND);
        }
        if (operator.getStatus() != SysUserStatus.ACTIVE) {
            throw new CustomException(ExceptionEnum.PLATFORM_OPERATOR_DISABLED);
        }
        List<String> roles = staffAuthorityMapper.selectRoleCodes(operatorId);
        List<Long> storeIds = staffAuthorityMapper.selectStoreIds(operatorId);
        Set<Long> normalizedStoreIds = storeIds == null
                ? Set.of()
                : storeIds.stream()
                .filter(Objects::nonNull)
                .filter(id -> id > 0)
                .collect(Collectors.toUnmodifiableSet());
        return new Authority(
                roles != null && roles.contains(RoleCode.SUPER_ADMIN.getCode()),
                roles != null && roles.contains(RoleCode.STORE_MANAGER.getCode()),
                normalizedStoreIds
        );
    }

    private void requireSuperAdmin(Authority authority) {
        if (!authority.superAdmin()) {
            throw new CustomException(ExceptionEnum.PLATFORM_ADMIN_ACCESS_DENIED);
        }
    }

    private void requireSettlementReader(Authority authority) {
        if (!authority.superAdmin() && !authority.storeManager()) {
            throw new CustomException(ExceptionEnum.PLATFORM_SETTLEMENT_ACCESS_DENIED);
        }
    }

    private void requireReadAccess(Authority authority, Long storeId) {
        if (!authority.superAdmin() && !authority.storeIds().contains(storeId)) {
            throw new CustomException(ExceptionEnum.PLATFORM_SETTLEMENT_ACCESS_DENIED);
        }
    }

    private void requireManagerAccess(Authority authority, Long storeId) {
        if (!authority.storeManager() || !authority.storeIds().contains(storeId)) {
            throw new CustomException(ExceptionEnum.PLATFORM_SETTLEMENT_ACCESS_DENIED);
        }
    }

    private YearMonth parsePeriodCode(String periodCode) {
        String normalized = normalizeRequired(periodCode);
        if (normalized == null || normalized.length() != 7) {
            throw new CustomException(ExceptionEnum.PLATFORM_SETTLEMENT_PERIOD_INVALID);
        }
        try {
            return YearMonth.parse(normalized, PERIOD_FORMATTER);
        } catch (DateTimeParseException ex) {
            throw new CustomException(ExceptionEnum.PLATFORM_SETTLEMENT_PERIOD_INVALID);
        }
    }

    private void validatePeriod(SettlementPeriod period, LocalDate startDate, LocalDate endDate) {
        if (period == null || period.getId() == null || period.getPeriodStatus() == null
                || !Objects.equals(period.getStartDate(), startDate)
                || !Objects.equals(period.getEndDate(), endDate)) {
            throw new CustomException(ExceptionEnum.PLATFORM_SETTLEMENT_DATA_INVALID);
        }
    }

    private NormalizedConfirm normalizeConfirm(SettlementConfirmCommand command) {
        if (command == null) {
            throw new CustomException(ExceptionEnum.PLATFORM_INVALID_REQUEST);
        }
        String settlementNo = normalizeRequired(command.settlementNo());
        String remark = normalizeOptional(command.remark());
        if (settlementNo == null || settlementNo.length() > SETTLEMENT_NO_MAX_LENGTH
                || (remark != null && remark.length() > REMARK_MAX_LENGTH)) {
            throw new CustomException(ExceptionEnum.PLATFORM_INVALID_REQUEST);
        }
        return new NormalizedConfirm(
                settlementNo,
                command.operatorId(),
                remark,
                normalizeClientIp(command.clientIp())
        );
    }

    private NormalizedPayment normalizePayment(SettlementPaymentCommand command) {
        if (command == null) {
            throw new CustomException(ExceptionEnum.PLATFORM_INVALID_REQUEST);
        }
        String settlementNo = normalizeRequired(command.settlementNo());
        String paymentReference = normalizeRequired(command.paymentReference());
        String remark = normalizeOptional(command.remark());
        if (paymentReference == null) {
            throw new CustomException(ExceptionEnum.PLATFORM_SETTLEMENT_PAYMENT_REFERENCE_REQUIRED);
        }
        if (settlementNo == null || settlementNo.length() > SETTLEMENT_NO_MAX_LENGTH
                || paymentReference.length() > PAYMENT_REFERENCE_MAX_LENGTH
                || (remark != null && remark.length() > REMARK_MAX_LENGTH)) {
            throw new CustomException(ExceptionEnum.PLATFORM_INVALID_REQUEST);
        }
        return new NormalizedPayment(
                settlementNo,
                command.operatorId(),
                paymentReference,
                remark,
                normalizeClientIp(command.clientIp())
        );
    }

    private String normalizeClientIp(String clientIp) {
        String normalized = normalizeOptional(clientIp);
        if (normalized != null && normalized.length() > 45) {
            throw new CustomException(ExceptionEnum.PLATFORM_INVALID_REQUEST);
        }
        return normalized;
    }

    private String normalizeRequired(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private void validatePage(int pageNum, int pageSize) {
        if (pageNum < 1 || pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw new CustomException(ExceptionEnum.PLATFORM_INVALID_REQUEST);
        }
    }

    private PageResult<StoreSettlementSummaryView> emptyPage(int pageNum, int pageSize) {
        return new PageResult<>(pageNum, pageSize, 0, 0, false, List.of());
    }

    private long requireValue(Long value) {
        if (value == null) {
            throw new CustomException(ExceptionEnum.PLATFORM_SETTLEMENT_DATA_INVALID);
        }
        return value;
    }

    private void requireOneRow(int affectedRows) {
        if (affectedRows != 1) {
            throw new CustomException(ExceptionEnum.PLATFORM_SETTLEMENT_WRITE_FAILED);
        }
    }

    private AuditLog buildGenerationAudit(
            SettlementPeriod period,
            Long operatorId,
            String clientIp,
            int orderCount,
            List<StoreSettlement> settlements
    ) {
        Map<String, Object> before = new LinkedHashMap<>();
        before.put("periodStatus", SettlementPeriodStatus.OPEN.getCode());

        Map<String, Object> after = new LinkedHashMap<>();
        after.put("periodStatus", SettlementPeriodStatus.GENERATED.getCode());
        after.put("orderCount", orderCount);
        after.put("settlementCount", settlements.size());
        after.put("grossAmountCent", sumSettlements(settlements, StoreSettlement::getGrossAmountCent));
        after.put("platformFeeCent", sumSettlements(settlements, StoreSettlement::getPlatformFeeCent));
        after.put("payableAmountCent", sumSettlements(settlements, StoreSettlement::getPayableAmountCent));

        return AuditLog.builder()
                .actorType(AuditActorType.SYS_USER)
                .actorId(operatorId)
                .operatorRole(RoleCode.SUPER_ADMIN)
                .action("SETTLEMENT_PERIOD_GENERATED")
                .resourceType("SETTLEMENT_PERIOD")
                .resourceNo(period.getPeriodCode())
                .requestId(period.getPeriodCode())
                .beforeSnapshot(writeJson(before))
                .afterSnapshot(writeJson(after))
                .clientIp(clientIp)
                .build();
    }

    private AuditLog buildSettlementAudit(
            String action,
            StoreSettlement settlement,
            StoreSettlementStatus beforeStatus,
            StoreSettlementStatus afterStatus,
            Long operatorId,
            RoleCode operatorRole,
            String clientIp,
            String remark,
            String paymentReference,
            LocalDateTime operationTime
    ) {
        Map<String, Object> before = new LinkedHashMap<>();
        before.put("settlementStatus", beforeStatus.getCode());
        before.put("payableAmountCent", settlement.getPayableAmountCent());

        Map<String, Object> after = new LinkedHashMap<>();
        after.put("settlementStatus", afterStatus.getCode());
        after.put("operationTime", operationTime);
        after.put("paymentReference", paymentReference);

        return AuditLog.builder()
                .actorType(AuditActorType.SYS_USER)
                .actorId(operatorId)
                .operatorRole(operatorRole)
                .storeId(settlement.getStoreId())
                .action(action)
                .resourceType("STORE_SETTLEMENT")
                .resourceNo(settlement.getSettlementNo())
                .requestId(paymentReference)
                .beforeSnapshot(writeJson(before))
                .afterSnapshot(writeJson(after))
                .remark(remark)
                .clientIp(clientIp)
                .build();
    }

    private long sumSettlements(
            List<StoreSettlement> settlements,
            Function<StoreSettlement, Long> getter
    ) {
        long total = 0;
        try {
            for (StoreSettlement settlement : settlements) {
                total = Math.addExact(total, requireValue(getter.apply(settlement)));
            }
        } catch (ArithmeticException ex) {
            throw new CustomException(ExceptionEnum.PLATFORM_SETTLEMENT_DATA_INVALID);
        }
        return total;
    }

    private String writeJson(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("结算审计快照序列化失败", ex);
        }
    }

    private record Authority(boolean superAdmin, boolean storeManager, Set<Long> storeIds) {
    }

    private record Totals(
            long points,
            long grossAmountCent,
            long platformFeeCent,
            long storePayableCent
    ) {
    }

    private record SettlementBatch(
            List<ConsumptionOrder> orders,
            List<ConsumptionOrder> reversals
    ) {
    }

    private record NormalizedConfirm(
            String settlementNo,
            Long operatorId,
            String remark,
            String clientIp
    ) {
    }

    private record NormalizedPayment(
            String settlementNo,
            Long operatorId,
            String paymentReference,
            String remark,
            String clientIp
    ) {
    }
}
