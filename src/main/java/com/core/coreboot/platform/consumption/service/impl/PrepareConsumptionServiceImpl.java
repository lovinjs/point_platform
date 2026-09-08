package com.core.coreboot.platform.consumption.service.impl;

import com.core.coreboot.exception.CustomException;
import com.core.coreboot.exception.ExceptionEnum;
import com.core.coreboot.platform.audit.entity.AuditLog;
import com.core.coreboot.platform.audit.mapper.AuditLogMapper;
import com.core.coreboot.platform.common.enums.AuditActorType;
import com.core.coreboot.platform.common.enums.ConsumptionOrderStatus;
import com.core.coreboot.platform.common.enums.ConsumptionVerificationMode;
import com.core.coreboot.platform.common.enums.CustomerStatus;
import com.core.coreboot.platform.common.enums.RoleCode;
import com.core.coreboot.platform.common.enums.SettlementStatus;
import com.core.coreboot.platform.common.model.PointMoneyPolicy;
import com.core.coreboot.platform.common.support.BusinessNoGenerator;
import com.core.coreboot.platform.consumption.config.ConsumptionProperties;
import com.core.coreboot.platform.consumption.entity.ConsumptionOrder;
import com.core.coreboot.platform.consumption.mapper.ConsumptionOrderMapper;
import com.core.coreboot.platform.consumption.model.PrepareConsumptionCommand;
import com.core.coreboot.platform.consumption.model.PrepareConsumptionResult;
import com.core.coreboot.platform.consumption.service.PrepareConsumptionService;
import com.core.coreboot.platform.customer.entity.CustomerUser;
import com.core.coreboot.platform.customer.entity.CustomerSecurity;
import com.core.coreboot.platform.customer.mapper.CustomerSecurityMapper;
import com.core.coreboot.platform.customer.mapper.CustomerUserMapper;
import com.core.coreboot.platform.point.entity.PointAccount;
import com.core.coreboot.platform.point.mapper.PointAccountMapper;
import com.core.coreboot.platform.staff.service.StaffStoreAuthorizationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@Service
public class PrepareConsumptionServiceImpl implements PrepareConsumptionService {
    private static final int IDEMPOTENCY_KEY_MAX_LENGTH = 64;
    private static final int REMARK_MAX_LENGTH = 500;
    private static final Duration MAXIMUM_PENDING_TTL = Duration.ofMinutes(30);

    private final CustomerUserMapper customerUserMapper;
    private final CustomerSecurityMapper customerSecurityMapper;
    private final StaffStoreAuthorizationService staffStoreAuthorizationService;
    private final PointAccountMapper pointAccountMapper;
    private final ConsumptionOrderMapper consumptionOrderMapper;
    private final AuditLogMapper auditLogMapper;
    private final ObjectMapper objectMapper;
    private final ConsumptionProperties properties;
    private final Clock clock;

    @Autowired
    public PrepareConsumptionServiceImpl(
            CustomerUserMapper customerUserMapper,
            CustomerSecurityMapper customerSecurityMapper,
            StaffStoreAuthorizationService staffStoreAuthorizationService,
            PointAccountMapper pointAccountMapper,
            ConsumptionOrderMapper consumptionOrderMapper,
            AuditLogMapper auditLogMapper,
            ObjectMapper objectMapper,
            ConsumptionProperties properties
    ) {
        this(
                customerUserMapper,
                customerSecurityMapper,
                staffStoreAuthorizationService,
                pointAccountMapper,
                consumptionOrderMapper,
                auditLogMapper,
                objectMapper,
                properties,
                Clock.systemDefaultZone()
        );
    }

    PrepareConsumptionServiceImpl(
            CustomerUserMapper customerUserMapper,
            CustomerSecurityMapper customerSecurityMapper,
            StaffStoreAuthorizationService staffStoreAuthorizationService,
            PointAccountMapper pointAccountMapper,
            ConsumptionOrderMapper consumptionOrderMapper,
            AuditLogMapper auditLogMapper,
            ObjectMapper objectMapper,
            ConsumptionProperties properties,
            Clock clock
    ) {
        this.customerUserMapper = customerUserMapper;
        this.customerSecurityMapper = customerSecurityMapper;
        this.staffStoreAuthorizationService = staffStoreAuthorizationService;
        this.pointAccountMapper = pointAccountMapper;
        this.consumptionOrderMapper = consumptionOrderMapper;
        this.auditLogMapper = auditLogMapper;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PrepareConsumptionResult prepare(PrepareConsumptionCommand command) {
        NormalizedPreparation normalized = validateAndNormalize(command);
        RoleCode effectiveRole = validateCustomerAndAccess(normalized);

        ConsumptionOrder existingOrder = consumptionOrderMapper.selectByIdempotencyKey(
                normalized.idempotencyKey()
        );
        if (existingOrder != null) {
            return replay(existingOrder, normalized);
        }

        PointAccount account = pointAccountMapper.selectByCustomerIdForUpdate(normalized.customerId());
        existingOrder = consumptionOrderMapper.selectByIdempotencyKey(normalized.idempotencyKey());
        if (existingOrder != null) {
            return replay(existingOrder, normalized);
        }

        LocalDateTime now = LocalDateTime.now(clock);
        consumptionOrderMapper.expirePendingByCustomerId(normalized.customerId(), now);
        if (consumptionOrderMapper.selectActivePendingByCustomerId(normalized.customerId()) != null) {
            throw new CustomException(ExceptionEnum.PLATFORM_CUSTOMER_HAS_PENDING_CONSUMPTION);
        }
        if (account == null
                || account.getAvailablePoints() == null
                || account.getAvailablePoints() < normalized.consumePoints()) {
            throw new CustomException(ExceptionEnum.PLATFORM_POINT_BALANCE_INSUFFICIENT);
        }

        ConsumptionOrder order = buildOrder(normalized, now);
        requireOneRow(consumptionOrderMapper.insert(order));
        requireOneRow(auditLogMapper.insert(buildAuditLog(order, normalized, effectiveRole)));
        return toResult(order);
    }

    private NormalizedPreparation validateAndNormalize(PrepareConsumptionCommand command) {
        if (command == null
                || !isPositive(command.customerId())
                || !isPositive(command.storeId())
                || !isPositive(command.operatorId())
                || !isPositive(command.consumePoints())) {
            throw new CustomException(ExceptionEnum.PLATFORM_CONSUMPTION_POINTS_INVALID);
        }

        PointMoneyPolicy.SettlementAmounts amounts;
        try {
            amounts = PointMoneyPolicy.settlementAmounts(
                    command.consumePoints(),
                    PointMoneyPolicy.DEFAULT_PLATFORM_FEE_RATE_BPS
            );
        } catch (IllegalArgumentException | ArithmeticException ex) {
            throw new CustomException(ExceptionEnum.PLATFORM_CONSUMPTION_POINTS_INVALID);
        }

        String idempotencyKey = normalizeRequired(command.idempotencyKey());
        String remark = normalizeOptional(command.remark());
        String clientIp = normalizeOptional(command.clientIp());
        if (idempotencyKey == null
                || idempotencyKey.length() > IDEMPOTENCY_KEY_MAX_LENGTH
                || (remark != null && remark.length() > REMARK_MAX_LENGTH)
                || (clientIp != null && clientIp.length() > 45)) {
            throw new CustomException(ExceptionEnum.PLATFORM_INVALID_REQUEST);
        }

        return new NormalizedPreparation(
                command.customerId(),
                command.storeId(),
                command.operatorId(),
                command.consumePoints(),
                amounts.grossAmountCent(),
                PointMoneyPolicy.DEFAULT_PLATFORM_FEE_RATE_BPS,
                amounts.platformFeeCent(),
                amounts.storePayableCent(),
                idempotencyKey,
                remark,
                clientIp,
                validatedPendingTtl()
        );
    }

    private RoleCode validateCustomerAndAccess(NormalizedPreparation command) {
        CustomerUser customer = customerUserMapper.selectById(command.customerId());
        if (customer == null) {
            throw new CustomException(ExceptionEnum.PLATFORM_CUSTOMER_NOT_FOUND);
        }
        if (customer.getStatus() != CustomerStatus.ACTIVE) {
            throw new CustomException(ExceptionEnum.PLATFORM_CUSTOMER_DISABLED);
        }
        if (customer.getPhone() == null || customer.getPhone().isBlank()) {
            throw new CustomException(ExceptionEnum.PLATFORM_PHONE_NOT_BOUND);
        }
        CustomerSecurity security = customerSecurityMapper.selectById(command.customerId());
        if (security == null
                || security.getConsumePinHash() == null
                || security.getConsumePinHash().isBlank()) {
            throw new CustomException(ExceptionEnum.PLATFORM_CONSUME_PIN_NOT_SET);
        }
        return staffStoreAuthorizationService.requireActiveStoreAccess(
                command.operatorId(),
                command.storeId()
        );
    }

    private PrepareConsumptionResult replay(
            ConsumptionOrder existingOrder,
            NormalizedPreparation command
    ) {
        boolean sameRequest = Objects.equals(existingOrder.getCustomerId(), command.customerId())
                && Objects.equals(existingOrder.getStoreId(), command.storeId())
                && Objects.equals(existingOrder.getOperatorId(), command.operatorId())
                && Objects.equals(existingOrder.getConsumePoints(), command.consumePoints())
                && Objects.equals(existingOrder.getGrossAmountCent(), command.grossAmountCent())
                && Objects.equals(existingOrder.getPlatformFeeRateBps(), command.platformFeeRateBps())
                && Objects.equals(existingOrder.getPlatformFeeCent(), command.platformFeeCent())
                && Objects.equals(existingOrder.getStorePayableCent(), command.storePayableCent())
                && existingOrder.getVerificationMode() == ConsumptionVerificationMode.CUSTOMER_PIN
                && Objects.equals(existingOrder.getRemark(), command.remark());
        if (!sameRequest) {
            throw new CustomException(ExceptionEnum.PLATFORM_IDEMPOTENCY_CONFLICT);
        }
        if (existingOrder.getOrderStatus() == ConsumptionOrderStatus.PENDING_CONFIRM
                && existingOrder.getExpiresTime() != null
                && !existingOrder.getExpiresTime().isAfter(LocalDateTime.now(clock))) {
            int expiredRows = consumptionOrderMapper.expirePendingByCustomerId(
                    existingOrder.getCustomerId(),
                    LocalDateTime.now(clock)
            );
            if (expiredRows > 0) {
                existingOrder.setOrderStatus(ConsumptionOrderStatus.EXPIRED);
            } else {
                ConsumptionOrder refreshed = consumptionOrderMapper.selectByIdempotencyKey(
                        command.idempotencyKey()
                );
                if (refreshed == null) {
                    throw new CustomException(ExceptionEnum.PLATFORM_CONSUMPTION_PREPARE_FAILED);
                }
                existingOrder = refreshed;
            }
        }
        return toResult(existingOrder);
    }

    private ConsumptionOrder buildOrder(NormalizedPreparation command, LocalDateTime now) {
        return ConsumptionOrder.builder()
                .orderNo(BusinessNoGenerator.next("CSM"))
                .customerId(command.customerId())
                .storeId(command.storeId())
                .consumePoints(command.consumePoints())
                .grossAmountCent(command.grossAmountCent())
                .platformFeeRateBps(command.platformFeeRateBps())
                .platformFeeCent(command.platformFeeCent())
                .storePayableCent(command.storePayableCent())
                .verificationMode(ConsumptionVerificationMode.CUSTOMER_PIN)
                .expiresTime(now.plus(command.pendingTtl()))
                .orderStatus(ConsumptionOrderStatus.PENDING_CONFIRM)
                .operatorId(command.operatorId())
                .settlementStatus(SettlementStatus.NOT_INCLUDED)
                .idempotencyKey(command.idempotencyKey())
                .remark(command.remark())
                .build();
    }

    private AuditLog buildAuditLog(
            ConsumptionOrder order,
            NormalizedPreparation command,
            RoleCode effectiveRole
    ) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("orderNo", order.getOrderNo());
        snapshot.put("customerId", command.customerId());
        snapshot.put("storeId", command.storeId());
        snapshot.put("consumePoints", command.consumePoints());
        snapshot.put("grossAmountCent", command.grossAmountCent());
        snapshot.put("platformFeeRateBps", command.platformFeeRateBps());
        snapshot.put("platformFeeCent", command.platformFeeCent());
        snapshot.put("storePayableCent", command.storePayableCent());
        snapshot.put("verificationMode", ConsumptionVerificationMode.CUSTOMER_PIN.getCode());
        snapshot.put("expiresTime", order.getExpiresTime().toString());
        snapshot.put("orderStatus", ConsumptionOrderStatus.PENDING_CONFIRM.getCode());

        return AuditLog.builder()
                .actorType(AuditActorType.SYS_USER)
                .actorId(command.operatorId())
                .operatorRole(effectiveRole)
                .storeId(command.storeId())
                .action("CONSUMPTION_ORDER_PREPARED")
                .resourceType("CONSUMPTION_ORDER")
                .resourceNo(order.getOrderNo())
                .requestId(command.idempotencyKey())
                .afterSnapshot(writeJson(snapshot))
                .remark(command.remark())
                .clientIp(command.clientIp())
                .build();
    }

    private PrepareConsumptionResult toResult(ConsumptionOrder order) {
        return new PrepareConsumptionResult(
                order.getOrderNo(),
                order.getCustomerId(),
                order.getStoreId(),
                order.getConsumePoints(),
                order.getGrossAmountCent(),
                order.getPlatformFeeRateBps(),
                order.getPlatformFeeCent(),
                order.getStorePayableCent(),
                order.getVerificationMode(),
                order.getExpiresTime(),
                order.getOrderStatus()
        );
    }

    private Duration validatedPendingTtl() {
        Duration pendingTtl = properties.getPendingTtl();
        if (pendingTtl == null
                || pendingTtl.isZero()
                || pendingTtl.isNegative()
                || pendingTtl.compareTo(MAXIMUM_PENDING_TTL) > 0) {
            throw new CustomException(ExceptionEnum.PLATFORM_CONSUMPTION_CONFIGURATION_INVALID);
        }
        return pendingTtl;
    }

    private String writeJson(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("审计快照序列化失败", ex);
        }
    }

    private void requireOneRow(int affectedRows) {
        if (affectedRows != 1) {
            throw new CustomException(ExceptionEnum.PLATFORM_CONSUMPTION_PREPARE_FAILED);
        }
    }

    private boolean isPositive(Long value) {
        return value != null && value > 0;
    }

    private String normalizeRequired(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private record NormalizedPreparation(
            Long customerId,
            Long storeId,
            Long operatorId,
            Long consumePoints,
            long grossAmountCent,
            int platformFeeRateBps,
            long platformFeeCent,
            long storePayableCent,
            String idempotencyKey,
            String remark,
            String clientIp,
            Duration pendingTtl
    ) {
    }
}
