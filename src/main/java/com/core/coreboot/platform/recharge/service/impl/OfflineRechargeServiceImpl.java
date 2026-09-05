package com.core.coreboot.platform.recharge.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.core.coreboot.exception.CustomException;
import com.core.coreboot.exception.ExceptionEnum;
import com.core.coreboot.platform.audit.entity.AuditLog;
import com.core.coreboot.platform.audit.mapper.AuditLogMapper;
import com.core.coreboot.platform.common.enums.AuditActorType;
import com.core.coreboot.platform.common.enums.CustomerStatus;
import com.core.coreboot.platform.common.enums.FundReceiver;
import com.core.coreboot.platform.common.enums.PaymentMethod;
import com.core.coreboot.platform.common.enums.PointLedgerBusinessType;
import com.core.coreboot.platform.common.enums.PointLedgerType;
import com.core.coreboot.platform.common.enums.PointLotStatus;
import com.core.coreboot.platform.common.enums.RechargeChannel;
import com.core.coreboot.platform.common.enums.RechargeOrderStatus;
import com.core.coreboot.platform.common.enums.RoleCode;
import com.core.coreboot.platform.common.enums.StoreStatus;
import com.core.coreboot.platform.common.enums.SysUserStatus;
import com.core.coreboot.platform.common.model.PointMoneyPolicy;
import com.core.coreboot.platform.common.support.BusinessNoGenerator;
import com.core.coreboot.platform.customer.entity.CustomerUser;
import com.core.coreboot.platform.customer.mapper.CustomerUserMapper;
import com.core.coreboot.platform.merchant.entity.Store;
import com.core.coreboot.platform.merchant.mapper.StoreMapper;
import com.core.coreboot.platform.point.entity.PointAccount;
import com.core.coreboot.platform.point.entity.PointLedger;
import com.core.coreboot.platform.point.entity.PointLot;
import com.core.coreboot.platform.point.mapper.PointAccountMapper;
import com.core.coreboot.platform.point.mapper.PointLedgerMapper;
import com.core.coreboot.platform.point.mapper.PointLotMapper;
import com.core.coreboot.platform.recharge.entity.RechargeOrder;
import com.core.coreboot.platform.recharge.mapper.RechargeOrderMapper;
import com.core.coreboot.platform.recharge.model.OfflineRechargeCommand;
import com.core.coreboot.platform.recharge.model.OfflineRechargeResult;
import com.core.coreboot.platform.recharge.service.OfflineRechargeService;
import com.core.coreboot.platform.staff.entity.SysUser;
import com.core.coreboot.platform.staff.mapper.StaffStoreAccessMapper;
import com.core.coreboot.platform.staff.mapper.SysUserMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class OfflineRechargeServiceImpl implements OfflineRechargeService {
    private static final int IDEMPOTENCY_KEY_MAX_LENGTH = 64;
    private static final int PAYMENT_REFERENCE_MAX_LENGTH = 128;
    private static final int REMARK_MAX_LENGTH = 500;

    private final CustomerUserMapper customerUserMapper;
    private final StoreMapper storeMapper;
    private final SysUserMapper sysUserMapper;
    private final StaffStoreAccessMapper staffStoreAccessMapper;
    private final PointAccountMapper pointAccountMapper;
    private final RechargeOrderMapper rechargeOrderMapper;
    private final PointLotMapper pointLotMapper;
    private final PointLedgerMapper pointLedgerMapper;
    private final AuditLogMapper auditLogMapper;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OfflineRechargeResult recharge(OfflineRechargeCommand command) {
        NormalizedRecharge normalized = validateAndNormalize(command);

        RechargeOrder existingOrder = findByIdempotencyKey(normalized.idempotencyKey());
        if (existingOrder != null) {
            return replay(existingOrder, normalized);
        }

        RoleCode effectiveRole = validateParticipantsAndAccess(normalized);

        pointAccountMapper.ensureAccount(normalized.customerId());
        PointAccount account = pointAccountMapper.selectByCustomerIdForUpdate(normalized.customerId());
        if (account == null) {
            throw new CustomException(ExceptionEnum.PLATFORM_POINT_ACCOUNT_ERROR);
        }

        // A concurrent retry for the same customer waits on the account lock, then reuses the first result.
        existingOrder = findByIdempotencyKey(normalized.idempotencyKey());
        if (existingOrder != null) {
            return replay(existingOrder, normalized);
        }
        rejectUsedPaymentReference(normalized);

        long balanceAfter;
        try {
            balanceAfter = Math.addExact(account.getAvailablePoints(), normalized.rechargePoints());
        } catch (ArithmeticException ex) {
            throw new CustomException(ExceptionEnum.PLATFORM_POINT_ACCOUNT_ERROR);
        }

        LocalDateTime now = LocalDateTime.now();
        RechargeOrder order = buildCompletedOrder(normalized, now);
        requireOneRow(rechargeOrderMapper.insert(order));

        requireOneRow(pointAccountMapper.increaseBalance(account.getId(), normalized.rechargePoints()));
        requireOneRow(pointLotMapper.insert(buildPointLot(order, normalized)));
        requireOneRow(pointLedgerMapper.insert(buildPointLedger(order, account, normalized, balanceAfter)));
        requireOneRow(auditLogMapper.insert(buildAuditLog(order, normalized, effectiveRole, balanceAfter)));

        return toResult(order);
    }

    private NormalizedRecharge validateAndNormalize(OfflineRechargeCommand command) {
        if (command == null
                || !isPositive(command.customerId())
                || !isPositive(command.storeId())
                || !isPositive(command.operatorId())
                || command.amountCent() == null
                || command.paymentMethod() == null) {
            throw new CustomException(ExceptionEnum.PLATFORM_INVALID_REQUEST);
        }

        long rechargePoints;
        try {
            rechargePoints = PointMoneyPolicy.pointsFromRechargeAmount(command.amountCent());
        } catch (IllegalArgumentException ex) {
            throw new CustomException(ExceptionEnum.PLATFORM_RECHARGE_AMOUNT_INVALID);
        }

        String paymentReference = normalizeRequired(command.paymentReference());
        if (paymentReference == null) {
            throw new CustomException(ExceptionEnum.PLATFORM_PAYMENT_REFERENCE_REQUIRED);
        }
        String idempotencyKey = normalizeRequired(command.idempotencyKey());
        if (idempotencyKey == null) {
            throw new CustomException(ExceptionEnum.PLATFORM_INVALID_REQUEST);
        }
        String remark = normalizeOptional(command.remark());
        if (paymentReference.length() > PAYMENT_REFERENCE_MAX_LENGTH
                || idempotencyKey.length() > IDEMPOTENCY_KEY_MAX_LENGTH
                || (remark != null && remark.length() > REMARK_MAX_LENGTH)) {
            throw new CustomException(ExceptionEnum.PLATFORM_INVALID_REQUEST);
        }

        return new NormalizedRecharge(
                command.customerId(),
                command.storeId(),
                command.operatorId(),
                command.amountCent(),
                rechargePoints,
                command.paymentMethod(),
                paymentReference,
                idempotencyKey,
                remark
        );
    }

    private RoleCode validateParticipantsAndAccess(NormalizedRecharge command) {
        CustomerUser customer = customerUserMapper.selectById(command.customerId());
        if (customer == null) {
            throw new CustomException(ExceptionEnum.PLATFORM_CUSTOMER_NOT_FOUND);
        }
        if (customer.getStatus() != CustomerStatus.ACTIVE) {
            throw new CustomException(ExceptionEnum.PLATFORM_CUSTOMER_DISABLED);
        }

        Store store = storeMapper.selectById(command.storeId());
        if (store == null) {
            throw new CustomException(ExceptionEnum.PLATFORM_STORE_NOT_FOUND);
        }
        if (store.getStatus() != StoreStatus.ACTIVE) {
            throw new CustomException(ExceptionEnum.PLATFORM_STORE_UNAVAILABLE);
        }

        SysUser operator = sysUserMapper.selectById(command.operatorId());
        if (operator == null) {
            throw new CustomException(ExceptionEnum.PLATFORM_OPERATOR_NOT_FOUND);
        }
        if (operator.getStatus() != SysUserStatus.ACTIVE) {
            throw new CustomException(ExceptionEnum.PLATFORM_OPERATOR_DISABLED);
        }

        String roleCode = staffStoreAccessMapper.findEffectiveRoleCode(command.operatorId(), command.storeId());
        if (roleCode == null) {
            throw new CustomException(ExceptionEnum.PLATFORM_STORE_ACCESS_DENIED);
        }
        try {
            return RoleCode.valueOf(roleCode);
        } catch (IllegalArgumentException ex) {
            throw new CustomException(ExceptionEnum.PLATFORM_STORE_ACCESS_DENIED);
        }
    }

    private RechargeOrder findByIdempotencyKey(String idempotencyKey) {
        return rechargeOrderMapper.selectOne(
                Wrappers.lambdaQuery(RechargeOrder.class)
                        .eq(RechargeOrder::getIdempotencyKey, idempotencyKey)
                        .last("LIMIT 1")
        );
    }

    private void rejectUsedPaymentReference(NormalizedRecharge command) {
        Long count = rechargeOrderMapper.selectCount(
                Wrappers.lambdaQuery(RechargeOrder.class)
                        .eq(RechargeOrder::getPaymentMethod, command.paymentMethod())
                        .eq(RechargeOrder::getPaymentReference, command.paymentReference())
        );
        if (count != null && count > 0) {
            throw new CustomException(ExceptionEnum.PLATFORM_PAYMENT_REFERENCE_USED);
        }
    }

    private OfflineRechargeResult replay(RechargeOrder existingOrder, NormalizedRecharge command) {
        boolean sameRequest = Objects.equals(existingOrder.getCustomerId(), command.customerId())
                && Objects.equals(existingOrder.getRechargeStoreId(), command.storeId())
                && Objects.equals(existingOrder.getOperatorId(), command.operatorId())
                && Objects.equals(existingOrder.getAmountCent(), command.amountCent())
                && Objects.equals(existingOrder.getRechargePoints(), command.rechargePoints())
                && existingOrder.getPaymentMethod() == command.paymentMethod()
                && Objects.equals(existingOrder.getPaymentReference(), command.paymentReference())
                && Objects.equals(existingOrder.getRemark(), command.remark())
                && existingOrder.getChannel() == RechargeChannel.OFFLINE
                && existingOrder.getFundReceiver() == FundReceiver.PLATFORM;
        if (!sameRequest || existingOrder.getOrderStatus() != RechargeOrderStatus.COMPLETED) {
            throw new CustomException(ExceptionEnum.PLATFORM_IDEMPOTENCY_CONFLICT);
        }
        return toResult(existingOrder);
    }

    private RechargeOrder buildCompletedOrder(NormalizedRecharge command, LocalDateTime now) {
        return RechargeOrder.builder()
                .orderNo(BusinessNoGenerator.next("RCH"))
                .customerId(command.customerId())
                .rechargeStoreId(command.storeId())
                .rechargePoints(command.rechargePoints())
                .amountCent(command.amountCent())
                .channel(RechargeChannel.OFFLINE)
                .paymentMethod(command.paymentMethod())
                .fundReceiver(FundReceiver.PLATFORM)
                .paymentReference(command.paymentReference())
                .orderStatus(RechargeOrderStatus.COMPLETED)
                .operatorId(command.operatorId())
                .paidTime(now)
                .completedTime(now)
                .idempotencyKey(command.idempotencyKey())
                .remark(command.remark())
                .build();
    }

    private PointLot buildPointLot(RechargeOrder order, NormalizedRecharge command) {
        return PointLot.builder()
                .customerId(command.customerId())
                .sourceRechargeOrderId(order.getId())
                .totalPoints(command.rechargePoints())
                .remainingPoints(command.rechargePoints())
                .lotStatus(PointLotStatus.AVAILABLE)
                .build();
    }

    private PointLedger buildPointLedger(
            RechargeOrder order,
            PointAccount account,
            NormalizedRecharge command,
            long balanceAfter
    ) {
        return PointLedger.builder()
                .ledgerNo(BusinessNoGenerator.next("LDG"))
                .accountId(account.getId())
                .customerId(command.customerId())
                .deltaPoints(command.rechargePoints())
                .balanceAfter(balanceAfter)
                .ledgerType(PointLedgerType.RECHARGE)
                .businessType(PointLedgerBusinessType.RECHARGE_ORDER)
                .businessNo(order.getOrderNo())
                .operatorId(command.operatorId())
                .storeId(command.storeId())
                .idempotencyKey(command.idempotencyKey())
                .remark(command.remark())
                .build();
    }

    private AuditLog buildAuditLog(
            RechargeOrder order,
            NormalizedRecharge command,
            RoleCode effectiveRole,
            long balanceAfter
    ) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("orderNo", order.getOrderNo());
        snapshot.put("customerId", command.customerId());
        snapshot.put("storeId", command.storeId());
        snapshot.put("amountCent", command.amountCent());
        snapshot.put("rechargePoints", command.rechargePoints());
        snapshot.put("paymentMethod", command.paymentMethod().getCode());
        snapshot.put("paymentReference", command.paymentReference());
        snapshot.put("fundReceiver", FundReceiver.PLATFORM.getCode());
        snapshot.put("balanceAfter", balanceAfter);

        return AuditLog.builder()
                .actorType(AuditActorType.SYS_USER)
                .actorId(command.operatorId())
                .operatorRole(effectiveRole)
                .storeId(command.storeId())
                .action("OFFLINE_RECHARGE_COMPLETED")
                .resourceType("RECHARGE_ORDER")
                .resourceNo(order.getOrderNo())
                .requestId(command.idempotencyKey())
                .afterSnapshot(writeJson(snapshot))
                .remark(command.remark())
                .build();
    }

    private String writeJson(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("审计快照序列化失败", ex);
        }
    }

    private OfflineRechargeResult toResult(RechargeOrder order) {
        return new OfflineRechargeResult(
                order.getOrderNo(),
                order.getCustomerId(),
                order.getRechargeStoreId(),
                order.getAmountCent(),
                order.getRechargePoints(),
                order.getOrderStatus()
        );
    }

    private void requireOneRow(int affectedRows) {
        if (affectedRows != 1) {
            throw new CustomException(ExceptionEnum.PLATFORM_RECHARGE_WRITE_FAILED);
        }
    }

    private boolean isPositive(Long value) {
        return value != null && value > 0;
    }

    private String normalizeRequired(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private record NormalizedRecharge(
            Long customerId,
            Long storeId,
            Long operatorId,
            Long amountCent,
            long rechargePoints,
            PaymentMethod paymentMethod,
            String paymentReference,
            String idempotencyKey,
            String remark
    ) {
    }
}
