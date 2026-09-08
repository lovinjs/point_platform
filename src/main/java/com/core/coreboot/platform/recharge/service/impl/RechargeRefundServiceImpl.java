package com.core.coreboot.platform.recharge.service.impl;

import com.core.coreboot.exception.CustomException;
import com.core.coreboot.exception.ExceptionEnum;
import com.core.coreboot.platform.audit.entity.AuditLog;
import com.core.coreboot.platform.audit.mapper.AuditLogMapper;
import com.core.coreboot.platform.common.enums.AuditActorType;
import com.core.coreboot.platform.common.enums.PointLedgerBusinessType;
import com.core.coreboot.platform.common.enums.PointLedgerType;
import com.core.coreboot.platform.common.enums.PointLotStatus;
import com.core.coreboot.platform.common.enums.RechargeOrderStatus;
import com.core.coreboot.platform.common.enums.RechargeRefundStatus;
import com.core.coreboot.platform.common.enums.RefundMethod;
import com.core.coreboot.platform.common.enums.RoleCode;
import com.core.coreboot.platform.common.enums.SysUserStatus;
import com.core.coreboot.platform.common.model.PointMoneyPolicy;
import com.core.coreboot.platform.common.support.BusinessNoGenerator;
import com.core.coreboot.platform.point.entity.PointAccount;
import com.core.coreboot.platform.point.entity.PointLedger;
import com.core.coreboot.platform.point.entity.PointLot;
import com.core.coreboot.platform.point.mapper.PointAccountMapper;
import com.core.coreboot.platform.point.mapper.PointLedgerMapper;
import com.core.coreboot.platform.point.mapper.PointLotMapper;
import com.core.coreboot.platform.point.mapper.PointLotUsageMapper;
import com.core.coreboot.platform.recharge.entity.RechargeOrder;
import com.core.coreboot.platform.recharge.entity.RechargeRefund;
import com.core.coreboot.platform.recharge.mapper.RechargeOrderMapper;
import com.core.coreboot.platform.recharge.mapper.RechargeRefundMapper;
import com.core.coreboot.platform.recharge.model.RechargeRefundCommand;
import com.core.coreboot.platform.recharge.model.RechargeRefundResult;
import com.core.coreboot.platform.recharge.service.RechargeRefundService;
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
public class RechargeRefundServiceImpl implements RechargeRefundService {
    private static final int ORDER_NO_MAX_LENGTH = 64;
    private static final int IDEMPOTENCY_KEY_MAX_LENGTH = 64;
    private static final int REFUND_REFERENCE_MAX_LENGTH = 128;
    private static final int REASON_MAX_LENGTH = 500;

    private final RechargeOrderMapper rechargeOrderMapper;
    private final RechargeRefundMapper rechargeRefundMapper;
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
    public RechargeRefundServiceImpl(
            RechargeOrderMapper rechargeOrderMapper,
            RechargeRefundMapper rechargeRefundMapper,
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
                rechargeOrderMapper,
                rechargeRefundMapper,
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

    RechargeRefundServiceImpl(
            RechargeOrderMapper rechargeOrderMapper,
            RechargeRefundMapper rechargeRefundMapper,
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
        this.rechargeOrderMapper = rechargeOrderMapper;
        this.rechargeRefundMapper = rechargeRefundMapper;
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
    public RechargeRefundResult refund(RechargeRefundCommand command) {
        NormalizedRefund normalized = validateAndNormalize(command);
        requireSuperAdmin(normalized.operatorId());

        RechargeRefund existingRefund = rechargeRefundMapper.selectByIdempotencyKey(
                normalized.idempotencyKey()
        );
        if (existingRefund != null) {
            return replay(existingRefund, normalized);
        }

        RechargeOrder snapshot = rechargeOrderMapper.selectByOrderNo(normalized.orderNo());
        if (snapshot == null) {
            throw new CustomException(ExceptionEnum.PLATFORM_RECHARGE_ORDER_NOT_FOUND);
        }
        requireOrderIdentity(snapshot);

        PointAccount account = pointAccountMapper.selectByCustomerIdForUpdate(snapshot.getCustomerId());
        if (account == null || account.getAvailablePoints() == null) {
            throw new CustomException(ExceptionEnum.PLATFORM_POINT_ACCOUNT_ERROR);
        }

        existingRefund = rechargeRefundMapper.selectByIdempotencyKey(normalized.idempotencyKey());
        if (existingRefund != null) {
            return replay(existingRefund, normalized, account);
        }

        RechargeOrder order = rechargeOrderMapper.selectByOrderNoForUpdate(normalized.orderNo());
        if (order == null) {
            throw new CustomException(ExceptionEnum.PLATFORM_RECHARGE_ORDER_NOT_FOUND);
        }
        requireOrderIdentity(order);
        if (!Objects.equals(order.getCustomerId(), account.getCustomerId())) {
            throw new CustomException(ExceptionEnum.PLATFORM_DATA_CONFLICT);
        }
        if (order.getOrderStatus() != RechargeOrderStatus.COMPLETED
                || rechargeRefundMapper.selectByRechargeOrderId(order.getId()) != null) {
            throw new CustomException(ExceptionEnum.PLATFORM_RECHARGE_NOT_REFUNDABLE);
        }

        PointLot lot = pointLotMapper.selectByRechargeOrderIdForUpdate(order.getId());
        validateUntouchedLot(order, lot);
        if (pointLotUsageMapper.countByPointLotId(lot.getId()) > 0) {
            throw new CustomException(ExceptionEnum.PLATFORM_RECHARGE_POINTS_ALREADY_USED);
        }
        if (account.getAvailablePoints() < order.getRechargePoints()) {
            throw new CustomException(ExceptionEnum.PLATFORM_POINT_BALANCE_INSUFFICIENT);
        }
        if (rechargeRefundMapper.countByRefundReference(
                normalized.refundMethod(),
                normalized.refundReference()
        ) > 0) {
            throw new CustomException(ExceptionEnum.PLATFORM_REFUND_REFERENCE_USED);
        }

        LocalDateTime completedTime = LocalDateTime.now(clock);
        long balanceAfter = account.getAvailablePoints() - order.getRechargePoints();
        RechargeRefund refund = buildRefund(order, normalized, completedTime);
        requireOneRow(rechargeRefundMapper.insert(refund));
        requireOneRow(pointLotMapper.markUntouchedLotRefunded(lot.getId(), order.getCustomerId()));
        requireOneRow(pointAccountMapper.decreaseBalance(account.getId(), order.getRechargePoints()));
        requireOneRow(rechargeOrderMapper.markRefunded(order.getId()));
        requireOneRow(pointLedgerMapper.insert(buildLedger(order, refund, account, balanceAfter)));
        requireOneRow(auditLogMapper.insert(buildAudit(
                order,
                refund,
                lot,
                normalized,
                account.getAvailablePoints(),
                balanceAfter
        )));

        order.setOrderStatus(RechargeOrderStatus.REFUNDED);
        return toResult(order, refund, balanceAfter);
    }

    private NormalizedRefund validateAndNormalize(RechargeRefundCommand command) {
        if (command == null || command.operatorId() == null || command.operatorId() <= 0
                || command.refundMethod() == null) {
            throw new CustomException(ExceptionEnum.PLATFORM_INVALID_REQUEST);
        }
        String orderNo = normalizeRequired(command.orderNo());
        String refundReference = normalizeRequired(command.refundReference());
        String reason = normalizeRequired(command.reason());
        String idempotencyKey = normalizeRequired(command.idempotencyKey());
        String clientIp = normalizeOptional(command.clientIp());
        if (refundReference == null) {
            throw new CustomException(ExceptionEnum.PLATFORM_REFUND_REFERENCE_REQUIRED);
        }
        if (orderNo == null || reason == null || idempotencyKey == null
                || orderNo.length() > ORDER_NO_MAX_LENGTH
                || refundReference.length() > REFUND_REFERENCE_MAX_LENGTH
                || reason.length() > REASON_MAX_LENGTH
                || idempotencyKey.length() > IDEMPOTENCY_KEY_MAX_LENGTH
                || (clientIp != null && clientIp.length() > 45)) {
            throw new CustomException(ExceptionEnum.PLATFORM_INVALID_REQUEST);
        }
        return new NormalizedRefund(
                orderNo,
                command.operatorId(),
                command.refundMethod(),
                refundReference,
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

    private void requireOrderIdentity(RechargeOrder order) {
        if (order.getId() == null || order.getCustomerId() == null || order.getCustomerId() <= 0
                || order.getRechargeStoreId() == null || order.getRechargeStoreId() <= 0
                || order.getRechargePoints() == null || order.getRechargePoints() <= 0
                || order.getAmountCent() == null || order.getAmountCent() <= 0) {
            throw new CustomException(ExceptionEnum.PLATFORM_RECHARGE_NOT_REFUNDABLE);
        }
        long expectedPoints;
        try {
            expectedPoints = PointMoneyPolicy.pointsFromRechargeAmount(order.getAmountCent());
        } catch (IllegalArgumentException ex) {
            throw new CustomException(ExceptionEnum.PLATFORM_RECHARGE_NOT_REFUNDABLE);
        }
        if (expectedPoints != order.getRechargePoints()) {
            throw new CustomException(ExceptionEnum.PLATFORM_RECHARGE_NOT_REFUNDABLE);
        }
    }

    private void validateUntouchedLot(RechargeOrder order, PointLot lot) {
        if (lot == null
                || !Objects.equals(lot.getCustomerId(), order.getCustomerId())
                || !Objects.equals(lot.getSourceRechargeOrderId(), order.getId())
                || lot.getLotStatus() != PointLotStatus.AVAILABLE
                || !Objects.equals(lot.getTotalPoints(), order.getRechargePoints())
                || !Objects.equals(lot.getRemainingPoints(), lot.getTotalPoints())) {
            throw new CustomException(ExceptionEnum.PLATFORM_RECHARGE_POINTS_ALREADY_USED);
        }
    }

    private RechargeRefund buildRefund(
            RechargeOrder order,
            NormalizedRefund command,
            LocalDateTime completedTime
    ) {
        return RechargeRefund.builder()
                .refundNo(BusinessNoGenerator.next("RFD"))
                .rechargeOrderId(order.getId())
                .customerId(order.getCustomerId())
                .refundPoints(order.getRechargePoints())
                .refundAmountCent(order.getAmountCent())
                .refundMethod(command.refundMethod())
                .refundReference(command.refundReference())
                .refundStatus(RechargeRefundStatus.COMPLETED)
                .operatorId(command.operatorId())
                .reason(command.reason())
                .completedTime(completedTime)
                .idempotencyKey(command.idempotencyKey())
                .build();
    }

    private PointLedger buildLedger(
            RechargeOrder order,
            RechargeRefund refund,
            PointAccount account,
            long balanceAfter
    ) {
        return PointLedger.builder()
                .ledgerNo(BusinessNoGenerator.next("LDG"))
                .accountId(account.getId())
                .customerId(order.getCustomerId())
                .deltaPoints(-order.getRechargePoints())
                .balanceAfter(balanceAfter)
                .ledgerType(PointLedgerType.REFUND)
                .businessType(PointLedgerBusinessType.RECHARGE_REFUND)
                .businessNo(refund.getRefundNo())
                .operatorId(refund.getOperatorId())
                .storeId(order.getRechargeStoreId())
                .idempotencyKey(refund.getIdempotencyKey())
                .remark(refund.getReason())
                .build();
    }

    private AuditLog buildAudit(
            RechargeOrder order,
            RechargeRefund refund,
            PointLot lot,
            NormalizedRefund command,
            long balanceBefore,
            long balanceAfter
    ) {
        Map<String, Object> before = new LinkedHashMap<>();
        before.put("rechargeOrderNo", order.getOrderNo());
        before.put("orderStatus", RechargeOrderStatus.COMPLETED.getCode());
        before.put("pointLotId", lot.getId());
        before.put("pointLotStatus", lot.getLotStatus().getCode());
        before.put("balance", balanceBefore);

        Map<String, Object> after = new LinkedHashMap<>();
        after.put("refundNo", refund.getRefundNo());
        after.put("refundPoints", refund.getRefundPoints());
        after.put("refundAmountCent", refund.getRefundAmountCent());
        after.put("refundMethod", refund.getRefundMethod().getCode());
        after.put("refundReference", refund.getRefundReference());
        after.put("refundStatus", refund.getRefundStatus().getCode());
        after.put("orderStatus", RechargeOrderStatus.REFUNDED.getCode());
        after.put("pointLotStatus", PointLotStatus.REFUNDED.getCode());
        after.put("balance", balanceAfter);

        return AuditLog.builder()
                .actorType(AuditActorType.SYS_USER)
                .actorId(command.operatorId())
                .operatorRole(RoleCode.SUPER_ADMIN)
                .storeId(order.getRechargeStoreId())
                .action("RECHARGE_REFUND_COMPLETED")
                .resourceType("RECHARGE_REFUND")
                .resourceNo(refund.getRefundNo())
                .requestId(command.idempotencyKey())
                .beforeSnapshot(writeJson(before))
                .afterSnapshot(writeJson(after))
                .remark(command.reason())
                .clientIp(command.clientIp())
                .build();
    }

    private RechargeRefundResult replay(
            RechargeRefund existingRefund,
            NormalizedRefund command
    ) {
        PointAccount account = pointAccountMapper.selectByCustomerId(existingRefund.getCustomerId());
        return replay(existingRefund, command, account);
    }

    private RechargeRefundResult replay(
            RechargeRefund existingRefund,
            NormalizedRefund command,
            PointAccount account
    ) {
        RechargeOrder order = rechargeOrderMapper.selectById(existingRefund.getRechargeOrderId());
        boolean sameRequest = order != null
                && Objects.equals(order.getOrderNo(), command.orderNo())
                && Objects.equals(existingRefund.getOperatorId(), command.operatorId())
                && existingRefund.getRefundMethod() == command.refundMethod()
                && Objects.equals(existingRefund.getRefundReference(), command.refundReference())
                && Objects.equals(existingRefund.getReason(), command.reason())
                && existingRefund.getRefundStatus() == RechargeRefundStatus.COMPLETED
                && order.getOrderStatus() == RechargeOrderStatus.REFUNDED;
        if (!sameRequest) {
            throw new CustomException(ExceptionEnum.PLATFORM_IDEMPOTENCY_CONFLICT);
        }
        if (account == null || account.getAvailablePoints() == null
                || !Objects.equals(account.getCustomerId(), existingRefund.getCustomerId())) {
            throw new CustomException(ExceptionEnum.PLATFORM_POINT_ACCOUNT_ERROR);
        }
        return toResult(order, existingRefund, account.getAvailablePoints());
    }

    private RechargeRefundResult toResult(
            RechargeOrder order,
            RechargeRefund refund,
            long availablePoints
    ) {
        return new RechargeRefundResult(
                refund.getRefundNo(),
                order.getOrderNo(),
                refund.getCustomerId(),
                refund.getRefundPoints(),
                refund.getRefundAmountCent(),
                refund.getRefundMethod(),
                refund.getRefundStatus(),
                availablePoints,
                refund.getCompletedTime()
        );
    }

    private String writeJson(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("充值退款审计快照序列化失败", ex);
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
            throw new CustomException(ExceptionEnum.PLATFORM_RECHARGE_REFUND_WRITE_FAILED);
        }
    }

    private record NormalizedRefund(
            String orderNo,
            Long operatorId,
            RefundMethod refundMethod,
            String refundReference,
            String reason,
            String idempotencyKey,
            String clientIp
    ) {
    }
}
