package com.core.coreboot.platform.consumption.service.impl;

import com.core.coreboot.exception.CustomException;
import com.core.coreboot.exception.ExceptionEnum;
import com.core.coreboot.platform.audit.entity.AuditLog;
import com.core.coreboot.platform.audit.mapper.AuditLogMapper;
import com.core.coreboot.platform.common.enums.AuditActorType;
import com.core.coreboot.platform.common.enums.ConsumptionOrderStatus;
import com.core.coreboot.platform.common.enums.ConsumptionVerificationMode;
import com.core.coreboot.platform.common.enums.MerchantStatus;
import com.core.coreboot.platform.common.enums.PointLedgerBusinessType;
import com.core.coreboot.platform.common.enums.PointLedgerType;
import com.core.coreboot.platform.common.enums.StoreStatus;
import com.core.coreboot.platform.common.support.BusinessNoGenerator;
import com.core.coreboot.platform.consumption.entity.ConsumptionOrder;
import com.core.coreboot.platform.consumption.mapper.ConsumptionOrderMapper;
import com.core.coreboot.platform.consumption.model.ConsumptionConfirmationOutcome;
import com.core.coreboot.platform.consumption.model.ConsumptionConfirmationResult;
import com.core.coreboot.platform.merchant.entity.Store;
import com.core.coreboot.platform.merchant.entity.Merchant;
import com.core.coreboot.platform.merchant.mapper.MerchantMapper;
import com.core.coreboot.platform.merchant.mapper.StoreMapper;
import com.core.coreboot.platform.point.entity.PointAccount;
import com.core.coreboot.platform.point.entity.PointLedger;
import com.core.coreboot.platform.point.entity.PointLot;
import com.core.coreboot.platform.point.entity.PointLotUsage;
import com.core.coreboot.platform.point.mapper.PointAccountMapper;
import com.core.coreboot.platform.point.mapper.PointLedgerMapper;
import com.core.coreboot.platform.point.mapper.PointLotMapper;
import com.core.coreboot.platform.point.mapper.PointLotUsageMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class ConsumptionConfirmationTransactionService {
    private final PointAccountMapper pointAccountMapper;
    private final ConsumptionOrderMapper consumptionOrderMapper;
    private final StoreMapper storeMapper;
    private final MerchantMapper merchantMapper;
    private final PointLotMapper pointLotMapper;
    private final PointLotUsageMapper pointLotUsageMapper;
    private final PointLedgerMapper pointLedgerMapper;
    private final AuditLogMapper auditLogMapper;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Autowired
    public ConsumptionConfirmationTransactionService(
            PointAccountMapper pointAccountMapper,
            ConsumptionOrderMapper consumptionOrderMapper,
            StoreMapper storeMapper,
            MerchantMapper merchantMapper,
            PointLotMapper pointLotMapper,
            PointLotUsageMapper pointLotUsageMapper,
            PointLedgerMapper pointLedgerMapper,
            AuditLogMapper auditLogMapper,
            ObjectMapper objectMapper
    ) {
        this(
                pointAccountMapper,
                consumptionOrderMapper,
                storeMapper,
                merchantMapper,
                pointLotMapper,
                pointLotUsageMapper,
                pointLedgerMapper,
                auditLogMapper,
                objectMapper,
                Clock.systemDefaultZone()
        );
    }

    ConsumptionConfirmationTransactionService(
            PointAccountMapper pointAccountMapper,
            ConsumptionOrderMapper consumptionOrderMapper,
            StoreMapper storeMapper,
            MerchantMapper merchantMapper,
            PointLotMapper pointLotMapper,
            PointLotUsageMapper pointLotUsageMapper,
            PointLedgerMapper pointLedgerMapper,
            AuditLogMapper auditLogMapper,
            ObjectMapper objectMapper,
            Clock clock
    ) {
        this.pointAccountMapper = pointAccountMapper;
        this.consumptionOrderMapper = consumptionOrderMapper;
        this.storeMapper = storeMapper;
        this.merchantMapper = merchantMapper;
        this.pointLotMapper = pointLotMapper;
        this.pointLotUsageMapper = pointLotUsageMapper;
        this.pointLedgerMapper = pointLedgerMapper;
        this.auditLogMapper = auditLogMapper;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Transactional(rollbackFor = Exception.class)
    public ConsumptionConfirmationOutcome confirmOrReplay(
            Long customerId,
            String orderNo,
            String clientIp
    ) {
        PointAccount account = pointAccountMapper.selectByCustomerIdForUpdate(customerId);
        if (account == null || account.getAvailablePoints() == null) {
            throw new CustomException(ExceptionEnum.PLATFORM_POINT_ACCOUNT_ERROR);
        }

        ConsumptionOrder order = consumptionOrderMapper.selectByOrderNoForUpdate(orderNo);
        requireOwnedOrder(order, customerId);

        Store store = storeMapper.selectByIdForUpdate(order.getStoreId());
        if (store == null) {
            throw new CustomException(ExceptionEnum.PLATFORM_STORE_NOT_FOUND);
        }

        if (order.getOrderStatus() == ConsumptionOrderStatus.COMPLETED) {
            return ConsumptionConfirmationOutcome.completed(toResult(order, store, account.getAvailablePoints()));
        }
        if (order.getOrderStatus() == ConsumptionOrderStatus.EXPIRED) {
            return ConsumptionConfirmationOutcome.expiredOrder();
        }
        if (order.getOrderStatus() != ConsumptionOrderStatus.PENDING_CONFIRM) {
            throw new CustomException(ExceptionEnum.PLATFORM_CONSUMPTION_ORDER_NOT_PENDING);
        }

        LocalDateTime now = LocalDateTime.now(clock);
        if (order.getExpiresTime() == null || !order.getExpiresTime().isAfter(now)) {
            requireOneRow(consumptionOrderMapper.expirePendingById(order.getId(), now));
            return ConsumptionConfirmationOutcome.expiredOrder();
        }
        if (order.getVerificationMode() != ConsumptionVerificationMode.CUSTOMER_PIN) {
            throw new CustomException(ExceptionEnum.PLATFORM_CONSUMPTION_ORDER_NOT_PENDING);
        }
        if (store.getStatus() != StoreStatus.ACTIVE) {
            throw new CustomException(ExceptionEnum.PLATFORM_STORE_UNAVAILABLE);
        }
        Merchant merchant = merchantMapper.selectByIdForUpdate(store.getMerchantId());
        if (merchant == null || merchant.getStatus() != MerchantStatus.ACTIVE) {
            throw new CustomException(ExceptionEnum.PLATFORM_MERCHANT_UNAVAILABLE);
        }

        long consumePoints = requirePositivePoints(order.getConsumePoints());
        if (account.getAvailablePoints() < consumePoints) {
            throw new CustomException(ExceptionEnum.PLATFORM_POINT_BALANCE_INSUFFICIENT);
        }

        List<PointLot> lots = pointLotMapper.selectAvailableByCustomerIdForUpdate(customerId);
        requireEnoughLotPoints(lots, consumePoints);
        consumeLots(order, customerId, lots, consumePoints);

        long balanceAfter = account.getAvailablePoints() - consumePoints;
        requireOneRow(pointAccountMapper.decreaseBalance(account.getId(), consumePoints));
        requireOneRow(pointLedgerMapper.insert(buildLedger(order, account, consumePoints, balanceAfter)));
        requireOneRow(consumptionOrderMapper.completePendingById(order.getId(), now));
        requireOneRow(auditLogMapper.insert(buildAudit(order, customerId, balanceAfter, clientIp, now)));

        order.setOrderStatus(ConsumptionOrderStatus.COMPLETED);
        order.setConfirmedTime(now);
        order.setCompletedTime(now);
        return ConsumptionConfirmationOutcome.completed(toResult(order, store, balanceAfter));
    }

    private void consumeLots(
            ConsumptionOrder order,
            Long customerId,
            List<PointLot> lots,
            long consumePoints
    ) {
        long remaining = consumePoints;
        for (PointLot lot : lots) {
            if (remaining == 0) {
                break;
            }
            long lotRemaining = lot.getRemainingPoints();
            long usedPoints = Math.min(remaining, lotRemaining);
            requireOneRow(pointLotMapper.consumePoints(lot.getId(), customerId, usedPoints));
            requireOneRow(pointLotUsageMapper.insert(PointLotUsage.builder()
                    .consumptionOrderId(order.getId())
                    .pointLotId(lot.getId())
                    .usedPoints(usedPoints)
                    .reversedPoints(0L)
                    .build()));
            remaining -= usedPoints;
        }
        if (remaining != 0) {
            throw new CustomException(ExceptionEnum.PLATFORM_POINT_ACCOUNT_ERROR);
        }
    }

    private void requireEnoughLotPoints(List<PointLot> lots, long consumePoints) {
        if (lots == null || lots.isEmpty()) {
            throw new CustomException(ExceptionEnum.PLATFORM_POINT_ACCOUNT_ERROR);
        }
        long total = 0;
        try {
            for (PointLot lot : lots) {
                if (lot == null
                        || lot.getId() == null
                        || lot.getRemainingPoints() == null
                        || lot.getRemainingPoints() <= 0) {
                    throw new CustomException(ExceptionEnum.PLATFORM_POINT_ACCOUNT_ERROR);
                }
                total = Math.addExact(total, lot.getRemainingPoints());
                if (total >= consumePoints) {
                    return;
                }
            }
        } catch (ArithmeticException ex) {
            throw new CustomException(ExceptionEnum.PLATFORM_POINT_ACCOUNT_ERROR);
        }
        throw new CustomException(ExceptionEnum.PLATFORM_POINT_ACCOUNT_ERROR);
    }

    private PointLedger buildLedger(
            ConsumptionOrder order,
            PointAccount account,
            long consumePoints,
            long balanceAfter
    ) {
        return PointLedger.builder()
                .ledgerNo(BusinessNoGenerator.next("LDG"))
                .accountId(account.getId())
                .customerId(order.getCustomerId())
                .deltaPoints(-consumePoints)
                .balanceAfter(balanceAfter)
                .ledgerType(PointLedgerType.CONSUME)
                .businessType(PointLedgerBusinessType.CONSUMPTION_ORDER)
                .businessNo(order.getOrderNo())
                .operatorId(order.getOperatorId())
                .storeId(order.getStoreId())
                .idempotencyKey(order.getOrderNo())
                .remark(order.getRemark())
                .build();
    }

    private AuditLog buildAudit(
            ConsumptionOrder order,
            Long customerId,
            long balanceAfter,
            String clientIp,
            LocalDateTime completedTime
    ) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("orderNo", order.getOrderNo());
        snapshot.put("customerId", customerId);
        snapshot.put("storeId", order.getStoreId());
        snapshot.put("consumePoints", order.getConsumePoints());
        snapshot.put("grossAmountCent", order.getGrossAmountCent());
        snapshot.put("platformFeeCent", order.getPlatformFeeCent());
        snapshot.put("storePayableCent", order.getStorePayableCent());
        snapshot.put("balanceAfter", balanceAfter);
        snapshot.put("orderStatus", ConsumptionOrderStatus.COMPLETED.getCode());
        snapshot.put("completedTime", completedTime.toString());

        return AuditLog.builder()
                .actorType(AuditActorType.CUSTOMER)
                .actorId(customerId)
                .storeId(order.getStoreId())
                .action("CONSUMPTION_ORDER_CONFIRMED")
                .resourceType("CONSUMPTION_ORDER")
                .resourceNo(order.getOrderNo())
                .requestId(order.getOrderNo())
                .afterSnapshot(writeJson(snapshot))
                .remark("客户本人输入消费密码确认消费")
                .clientIp(normalizeClientIp(clientIp))
                .build();
    }

    private ConsumptionConfirmationResult toResult(
            ConsumptionOrder order,
            Store store,
            long availablePoints
    ) {
        return new ConsumptionConfirmationResult(
                order.getOrderNo(),
                order.getStoreId(),
                store.getStoreName(),
                requirePositivePoints(order.getConsumePoints()),
                requireNonNegative(order.getGrossAmountCent()),
                availablePoints,
                order.getOrderStatus(),
                order.getCompletedTime()
        );
    }

    private void requireOwnedOrder(ConsumptionOrder order, Long customerId) {
        if (order == null || !Objects.equals(order.getCustomerId(), customerId)) {
            throw new CustomException(ExceptionEnum.PLATFORM_CONSUMPTION_ORDER_NOT_FOUND);
        }
    }

    private long requirePositivePoints(Long value) {
        if (value == null || value <= 0) {
            throw new CustomException(ExceptionEnum.PLATFORM_CONSUMPTION_CONFIRM_FAILED);
        }
        return value;
    }

    private long requireNonNegative(Long value) {
        if (value == null || value < 0) {
            throw new CustomException(ExceptionEnum.PLATFORM_CONSUMPTION_CONFIRM_FAILED);
        }
        return value;
    }

    private String writeJson(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("消费确认审计快照序列化失败", ex);
        }
    }

    private String normalizeClientIp(String clientIp) {
        if (clientIp == null || clientIp.isBlank()) {
            return null;
        }
        String normalized = clientIp.trim();
        return normalized.length() <= 45 ? normalized : normalized.substring(0, 45);
    }

    private void requireOneRow(int affectedRows) {
        if (affectedRows != 1) {
            throw new CustomException(ExceptionEnum.PLATFORM_CONSUMPTION_CONFIRM_FAILED);
        }
    }
}
