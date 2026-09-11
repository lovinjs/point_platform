package com.core.coreboot.platform.consumption.service.impl;

import com.core.coreboot.exception.CustomException;
import com.core.coreboot.exception.ExceptionEnum;
import com.core.coreboot.platform.audit.entity.AuditLog;
import com.core.coreboot.platform.audit.mapper.AuditLogMapper;
import com.core.coreboot.platform.common.enums.AuditActorType;
import com.core.coreboot.platform.common.enums.ConsumptionOrderStatus;
import com.core.coreboot.platform.common.enums.PointLedgerBusinessType;
import com.core.coreboot.platform.common.enums.PointLedgerType;
import com.core.coreboot.platform.common.enums.PointLotStatus;
import com.core.coreboot.platform.common.enums.RoleCode;
import com.core.coreboot.platform.common.enums.SettlementStatus;
import com.core.coreboot.platform.common.enums.SysUserStatus;
import com.core.coreboot.platform.common.support.BusinessNoGenerator;
import com.core.coreboot.platform.consumption.entity.ConsumptionOrder;
import com.core.coreboot.platform.consumption.mapper.ConsumptionOrderMapper;
import com.core.coreboot.platform.consumption.model.ConsumptionReversalCommand;
import com.core.coreboot.platform.consumption.model.ConsumptionReversalResult;
import com.core.coreboot.platform.consumption.service.ConsumptionReversalService;
import com.core.coreboot.platform.point.entity.PointAccount;
import com.core.coreboot.platform.point.entity.PointLedger;
import com.core.coreboot.platform.point.entity.PointLot;
import com.core.coreboot.platform.point.entity.PointLotUsage;
import com.core.coreboot.platform.point.mapper.PointAccountMapper;
import com.core.coreboot.platform.point.mapper.PointLedgerMapper;
import com.core.coreboot.platform.point.mapper.PointLotMapper;
import com.core.coreboot.platform.point.mapper.PointLotUsageMapper;
import com.core.coreboot.platform.staff.entity.SysUser;
import com.core.coreboot.platform.staff.mapper.StaffAuthorityMapper;
import com.core.coreboot.platform.staff.mapper.SysUserMapper;
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
public class ConsumptionReversalServiceImpl implements ConsumptionReversalService {
    private static final int ORDER_NO_MAX_LENGTH = 64;
    private static final int IDEMPOTENCY_KEY_MAX_LENGTH = 64;
    private static final int REASON_MAX_LENGTH = 500;
    private static final int CLIENT_IP_MAX_LENGTH = 45;

    private final ConsumptionOrderMapper consumptionOrderMapper;
    private final PointAccountMapper pointAccountMapper;
    private final PointLotMapper pointLotMapper;
    private final PointLotUsageMapper pointLotUsageMapper;
    private final PointLedgerMapper pointLedgerMapper;
    private final SysUserMapper sysUserMapper;
    private final StaffAuthorityMapper staffAuthorityMapper;
    private final AuditLogMapper auditLogMapper;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Autowired
    public ConsumptionReversalServiceImpl(
            ConsumptionOrderMapper consumptionOrderMapper,
            PointAccountMapper pointAccountMapper,
            PointLotMapper pointLotMapper,
            PointLotUsageMapper pointLotUsageMapper,
            PointLedgerMapper pointLedgerMapper,
            SysUserMapper sysUserMapper,
            StaffAuthorityMapper staffAuthorityMapper,
            AuditLogMapper auditLogMapper,
            ObjectMapper objectMapper
    ) {
        this(
                consumptionOrderMapper,
                pointAccountMapper,
                pointLotMapper,
                pointLotUsageMapper,
                pointLedgerMapper,
                sysUserMapper,
                staffAuthorityMapper,
                auditLogMapper,
                objectMapper,
                Clock.systemDefaultZone()
        );
    }

    ConsumptionReversalServiceImpl(
            ConsumptionOrderMapper consumptionOrderMapper,
            PointAccountMapper pointAccountMapper,
            PointLotMapper pointLotMapper,
            PointLotUsageMapper pointLotUsageMapper,
            PointLedgerMapper pointLedgerMapper,
            SysUserMapper sysUserMapper,
            StaffAuthorityMapper staffAuthorityMapper,
            AuditLogMapper auditLogMapper,
            ObjectMapper objectMapper,
            Clock clock
    ) {
        this.consumptionOrderMapper = consumptionOrderMapper;
        this.pointAccountMapper = pointAccountMapper;
        this.pointLotMapper = pointLotMapper;
        this.pointLotUsageMapper = pointLotUsageMapper;
        this.pointLedgerMapper = pointLedgerMapper;
        this.sysUserMapper = sysUserMapper;
        this.staffAuthorityMapper = staffAuthorityMapper;
        this.auditLogMapper = auditLogMapper;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ConsumptionReversalResult reverse(ConsumptionReversalCommand command) {
        NormalizedReversal normalized = validateAndNormalize(command);
        requireSuperAdmin(normalized.operatorId());

        PointLedger existingLedger = pointLedgerMapper.selectByIdempotencyKey(
                normalized.idempotencyKey()
        );
        if (existingLedger != null) {
            return replay(existingLedger, normalized);
        }

        ConsumptionOrder snapshot = consumptionOrderMapper.selectByOrderNo(normalized.orderNo());
        if (snapshot == null) {
            throw new CustomException(ExceptionEnum.PLATFORM_CONSUMPTION_ORDER_NOT_FOUND);
        }
        validateOrderIdentity(snapshot);

        PointAccount account = pointAccountMapper.selectByCustomerIdForUpdate(snapshot.getCustomerId());
        validateAccount(account, snapshot.getCustomerId());

        existingLedger = pointLedgerMapper.selectByIdempotencyKey(normalized.idempotencyKey());
        if (existingLedger != null) {
            return replay(existingLedger, normalized, account);
        }

        ConsumptionOrder order = consumptionOrderMapper.selectByOrderNoForUpdate(normalized.orderNo());
        if (order == null) {
            throw new CustomException(ExceptionEnum.PLATFORM_CONSUMPTION_ORDER_NOT_FOUND);
        }
        validateOrderIdentity(order);
        if (!Objects.equals(order.getCustomerId(), account.getCustomerId())) {
            throw new CustomException(ExceptionEnum.PLATFORM_DATA_CONFLICT);
        }
        if (order.getOrderStatus() != ConsumptionOrderStatus.COMPLETED
                || !isReversibleSettlementStatus(order.getSettlementStatus())) {
            throw new CustomException(ExceptionEnum.PLATFORM_CONSUMPTION_REVERSAL_NOT_ALLOWED);
        }

        List<PointLotUsage> usages = pointLotUsageMapper.selectByConsumptionOrderIdForUpdate(
                order.getId()
        );
        validateUsages(order, usages);
        restoreLots(order, usages);

        long balanceBefore = account.getAvailablePoints();
        long balanceAfter;
        try {
            balanceAfter = Math.addExact(balanceBefore, order.getConsumePoints());
        } catch (ArithmeticException ex) {
            throw new CustomException(ExceptionEnum.PLATFORM_CONSUMPTION_REVERSAL_DATA_INVALID);
        }

        LocalDateTime reversedTime = LocalDateTime.now(clock);
        SettlementStatus settlementStatusBefore = order.getSettlementStatus();
        SettlementStatus settlementStatusAfter = settlementStatusBefore == SettlementStatus.NOT_INCLUDED
                ? SettlementStatus.ADJUSTED
                : settlementStatusBefore;
        requireOneRow(pointAccountMapper.increaseBalance(account.getId(), order.getConsumePoints()));
        requireOneRow(consumptionOrderMapper.markReversed(
                order.getId(),
                normalized.operatorId(),
                reversedTime,
                normalized.reason()
        ));
        PointLedger ledger = buildLedger(order, account, normalized, balanceAfter);
        requireOneRow(pointLedgerMapper.insert(ledger));
        requireOneRow(auditLogMapper.insert(buildAudit(
                order,
                ledger,
                normalized,
                balanceBefore,
                balanceAfter,
                settlementStatusBefore,
                settlementStatusAfter,
                reversedTime
        )));

        order.setOrderStatus(ConsumptionOrderStatus.REVERSED);
        order.setReversedBy(normalized.operatorId());
        order.setReversedTime(reversedTime);
        order.setReversalReason(normalized.reason());
        order.setSettlementStatus(settlementStatusAfter);
        return toResult(order, ledger, balanceAfter);
    }

    private NormalizedReversal validateAndNormalize(ConsumptionReversalCommand command) {
        if (command == null || command.operatorId() == null || command.operatorId() <= 0) {
            throw new CustomException(ExceptionEnum.PLATFORM_INVALID_REQUEST);
        }
        String orderNo = normalizeRequired(command.orderNo());
        String reason = normalizeRequired(command.reason());
        String idempotencyKey = normalizeRequired(command.idempotencyKey());
        String clientIp = normalizeOptional(command.clientIp());
        if (orderNo == null || reason == null || idempotencyKey == null
                || orderNo.length() > ORDER_NO_MAX_LENGTH
                || reason.length() > REASON_MAX_LENGTH
                || idempotencyKey.length() > IDEMPOTENCY_KEY_MAX_LENGTH
                || (clientIp != null && clientIp.length() > CLIENT_IP_MAX_LENGTH)) {
            throw new CustomException(ExceptionEnum.PLATFORM_INVALID_REQUEST);
        }
        return new NormalizedReversal(
                orderNo,
                command.operatorId(),
                reason,
                idempotencyKey,
                clientIp
        );
    }

    private void requireSuperAdmin(Long operatorId) {
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

    private void validateOrderIdentity(ConsumptionOrder order) {
        if (order.getId() == null || order.getCustomerId() == null || order.getCustomerId() <= 0
                || order.getStoreId() == null || order.getStoreId() <= 0
                || order.getConsumePoints() == null || order.getConsumePoints() <= 0
                || order.getGrossAmountCent() == null || order.getGrossAmountCent() <= 0
                || order.getPlatformFeeCent() == null || order.getPlatformFeeCent() < 0
                || order.getStorePayableCent() == null || order.getStorePayableCent() < 0
                || order.getSettlementStatus() == null) {
            throw new CustomException(ExceptionEnum.PLATFORM_CONSUMPTION_REVERSAL_DATA_INVALID);
        }
        try {
            long expectedGross = Math.multiplyExact(order.getConsumePoints(), 100L);
            long splitTotal = Math.addExact(order.getPlatformFeeCent(), order.getStorePayableCent());
            if (expectedGross != order.getGrossAmountCent() || splitTotal != expectedGross) {
                throw new CustomException(ExceptionEnum.PLATFORM_CONSUMPTION_REVERSAL_DATA_INVALID);
            }
        } catch (ArithmeticException ex) {
            throw new CustomException(ExceptionEnum.PLATFORM_CONSUMPTION_REVERSAL_DATA_INVALID);
        }
    }

    private void validateAccount(PointAccount account, Long customerId) {
        if (account == null || account.getId() == null
                || !Objects.equals(account.getCustomerId(), customerId)
                || account.getAvailablePoints() == null || account.getAvailablePoints() < 0) {
            throw new CustomException(ExceptionEnum.PLATFORM_POINT_ACCOUNT_ERROR);
        }
    }

    private void validateUsages(ConsumptionOrder order, List<PointLotUsage> usages) {
        if (usages == null || usages.isEmpty()) {
            throw new CustomException(ExceptionEnum.PLATFORM_CONSUMPTION_REVERSAL_DATA_INVALID);
        }
        long totalUsed = 0L;
        try {
            for (PointLotUsage usage : usages) {
                if (usage == null || usage.getId() == null || usage.getPointLotId() == null
                        || !Objects.equals(usage.getConsumptionOrderId(), order.getId())
                        || usage.getUsedPoints() == null || usage.getUsedPoints() <= 0
                        || usage.getReversedPoints() == null || usage.getReversedPoints() != 0) {
                    throw new CustomException(ExceptionEnum.PLATFORM_CONSUMPTION_REVERSAL_DATA_INVALID);
                }
                totalUsed = Math.addExact(totalUsed, usage.getUsedPoints());
            }
        } catch (ArithmeticException ex) {
            throw new CustomException(ExceptionEnum.PLATFORM_CONSUMPTION_REVERSAL_DATA_INVALID);
        }
        if (totalUsed != order.getConsumePoints()) {
            throw new CustomException(ExceptionEnum.PLATFORM_CONSUMPTION_REVERSAL_DATA_INVALID);
        }
    }

    private void restoreLots(ConsumptionOrder order, List<PointLotUsage> usages) {
        for (PointLotUsage usage : usages) {
            PointLot lot = pointLotMapper.selectByIdForUpdate(usage.getPointLotId());
            validateRestorableLot(order, usage, lot);
            requireOneRow(pointLotMapper.restorePoints(
                    lot.getId(),
                    order.getCustomerId(),
                    usage.getUsedPoints()
            ));
            requireOneRow(pointLotUsageMapper.markFullyReversed(
                    usage.getId(),
                    order.getId(),
                    usage.getUsedPoints()
            ));
        }
    }

    private void validateRestorableLot(
            ConsumptionOrder order,
            PointLotUsage usage,
            PointLot lot
    ) {
        if (lot == null || lot.getId() == null
                || !Objects.equals(lot.getId(), usage.getPointLotId())
                || !Objects.equals(lot.getCustomerId(), order.getCustomerId())
                || lot.getTotalPoints() == null || lot.getTotalPoints() <= 0
                || lot.getRemainingPoints() == null || lot.getRemainingPoints() < 0
                || (lot.getLotStatus() != PointLotStatus.AVAILABLE
                && lot.getLotStatus() != PointLotStatus.DEPLETED)) {
            throw new CustomException(ExceptionEnum.PLATFORM_CONSUMPTION_REVERSAL_DATA_INVALID);
        }
        try {
            long restoredRemaining = Math.addExact(lot.getRemainingPoints(), usage.getUsedPoints());
            if (restoredRemaining > lot.getTotalPoints()) {
                throw new CustomException(ExceptionEnum.PLATFORM_CONSUMPTION_REVERSAL_DATA_INVALID);
            }
        } catch (ArithmeticException ex) {
            throw new CustomException(ExceptionEnum.PLATFORM_CONSUMPTION_REVERSAL_DATA_INVALID);
        }
    }

    private boolean isReversibleSettlementStatus(SettlementStatus status) {
        return status == SettlementStatus.NOT_INCLUDED
                || status == SettlementStatus.INCLUDED
                || status == SettlementStatus.SETTLED;
    }

    private PointLedger buildLedger(
            ConsumptionOrder order,
            PointAccount account,
            NormalizedReversal command,
            long balanceAfter
    ) {
        return PointLedger.builder()
                .ledgerNo(BusinessNoGenerator.next("LDG"))
                .accountId(account.getId())
                .customerId(order.getCustomerId())
                .deltaPoints(order.getConsumePoints())
                .balanceAfter(balanceAfter)
                .ledgerType(PointLedgerType.REVERSAL)
                .businessType(PointLedgerBusinessType.CONSUMPTION_ORDER)
                .businessNo(order.getOrderNo())
                .operatorId(command.operatorId())
                .storeId(order.getStoreId())
                .idempotencyKey(command.idempotencyKey())
                .remark(command.reason())
                .build();
    }

    private AuditLog buildAudit(
            ConsumptionOrder order,
            PointLedger ledger,
            NormalizedReversal command,
            long balanceBefore,
            long balanceAfter,
            SettlementStatus settlementStatusBefore,
            SettlementStatus settlementStatusAfter,
            LocalDateTime reversedTime
    ) {
        Map<String, Object> before = new LinkedHashMap<>();
        before.put("orderStatus", ConsumptionOrderStatus.COMPLETED.getCode());
        before.put("settlementStatus", settlementStatusBefore.getCode());
        before.put("balance", balanceBefore);

        Map<String, Object> after = new LinkedHashMap<>();
        after.put("orderStatus", ConsumptionOrderStatus.REVERSED.getCode());
        after.put("settlementStatus", settlementStatusAfter.getCode());
        after.put("reversedPoints", order.getConsumePoints());
        after.put("balance", balanceAfter);
        after.put("ledgerNo", ledger.getLedgerNo());
        after.put("reversedTime", reversedTime);

        return AuditLog.builder()
                .actorType(AuditActorType.SYS_USER)
                .actorId(command.operatorId())
                .operatorRole(RoleCode.SUPER_ADMIN)
                .storeId(order.getStoreId())
                .action("CONSUMPTION_ORDER_REVERSED")
                .resourceType("CONSUMPTION_ORDER")
                .resourceNo(order.getOrderNo())
                .requestId(command.idempotencyKey())
                .beforeSnapshot(writeJson(before))
                .afterSnapshot(writeJson(after))
                .remark(command.reason())
                .clientIp(command.clientIp())
                .build();
    }

    private ConsumptionReversalResult replay(
            PointLedger existingLedger,
            NormalizedReversal command
    ) {
        PointAccount account = pointAccountMapper.selectByCustomerId(existingLedger.getCustomerId());
        return replay(existingLedger, command, account);
    }

    private ConsumptionReversalResult replay(
            PointLedger existingLedger,
            NormalizedReversal command,
            PointAccount account
    ) {
        ConsumptionOrder order = consumptionOrderMapper.selectByOrderNo(command.orderNo());
        boolean sameRequest = order != null
                && existingLedger.getLedgerType() == PointLedgerType.REVERSAL
                && existingLedger.getBusinessType() == PointLedgerBusinessType.CONSUMPTION_ORDER
                && Objects.equals(existingLedger.getBusinessNo(), command.orderNo())
                && Objects.equals(existingLedger.getCustomerId(), order.getCustomerId())
                && Objects.equals(existingLedger.getStoreId(), order.getStoreId())
                && Objects.equals(existingLedger.getOperatorId(), command.operatorId())
                && Objects.equals(existingLedger.getRemark(), command.reason())
                && Objects.equals(existingLedger.getDeltaPoints(), order.getConsumePoints())
                && order.getOrderStatus() == ConsumptionOrderStatus.REVERSED;
        if (!sameRequest) {
            throw new CustomException(ExceptionEnum.PLATFORM_IDEMPOTENCY_CONFLICT);
        }
        validateAccount(account, order.getCustomerId());
        return toResult(order, existingLedger, account.getAvailablePoints());
    }

    private ConsumptionReversalResult toResult(
            ConsumptionOrder order,
            PointLedger ledger,
            long availablePoints
    ) {
        return new ConsumptionReversalResult(
                ledger.getLedgerNo(),
                order.getOrderNo(),
                order.getCustomerId(),
                order.getStoreId(),
                order.getConsumePoints(),
                availablePoints,
                order.getSettlementStatus(),
                order.getReversedTime()
        );
    }

    private String writeJson(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("消费冲正审计快照序列化失败", ex);
        }
    }

    private String normalizeRequired(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private void requireOneRow(int affectedRows) {
        if (affectedRows != 1) {
            throw new CustomException(ExceptionEnum.PLATFORM_CONSUMPTION_REVERSAL_WRITE_FAILED);
        }
    }

    private record NormalizedReversal(
            String orderNo,
            Long operatorId,
            String reason,
            String idempotencyKey,
            String clientIp
    ) {
    }
}
