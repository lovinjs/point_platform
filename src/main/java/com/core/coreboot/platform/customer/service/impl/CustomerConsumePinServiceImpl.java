package com.core.coreboot.platform.customer.service.impl;

import com.core.coreboot.exception.CustomException;
import com.core.coreboot.exception.ExceptionEnum;
import com.core.coreboot.platform.audit.entity.AuditLog;
import com.core.coreboot.platform.audit.mapper.AuditLogMapper;
import com.core.coreboot.platform.common.enums.AuditActorType;
import com.core.coreboot.platform.common.enums.CustomerStatus;
import com.core.coreboot.platform.customer.config.CustomerPinProperties;
import com.core.coreboot.platform.customer.entity.CustomerSecurity;
import com.core.coreboot.platform.customer.entity.CustomerUser;
import com.core.coreboot.platform.customer.mapper.CustomerSecurityMapper;
import com.core.coreboot.platform.customer.mapper.CustomerUserMapper;
import com.core.coreboot.platform.customer.model.ConsumePinStatus;
import com.core.coreboot.platform.customer.service.CustomerConsumePinService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class CustomerConsumePinServiceImpl implements CustomerConsumePinService {
    private static final Pattern PIN_PATTERN = Pattern.compile("\\d{6}");
    private static final Set<String> WEAK_PINS = Set.of(
            "000000", "111111", "222222", "333333", "444444",
            "555555", "666666", "777777", "888888", "999999",
            "012345", "123456", "234567", "345678", "456789",
            "987654", "876543", "765432", "654321", "543210"
    );

    private final CustomerUserMapper customerUserMapper;
    private final CustomerSecurityMapper customerSecurityMapper;
    private final AuditLogMapper auditLogMapper;
    private final PasswordEncoder passwordEncoder;
    private final CustomerPinProperties properties;
    private final Clock clock;

    @Autowired
    public CustomerConsumePinServiceImpl(
            CustomerUserMapper customerUserMapper,
            CustomerSecurityMapper customerSecurityMapper,
            AuditLogMapper auditLogMapper,
            PasswordEncoder passwordEncoder,
            CustomerPinProperties properties
    ) {
        this(
                customerUserMapper,
                customerSecurityMapper,
                auditLogMapper,
                passwordEncoder,
                properties,
                Clock.systemDefaultZone()
        );
    }

    CustomerConsumePinServiceImpl(
            CustomerUserMapper customerUserMapper,
            CustomerSecurityMapper customerSecurityMapper,
            AuditLogMapper auditLogMapper,
            PasswordEncoder passwordEncoder,
            CustomerPinProperties properties,
            Clock clock
    ) {
        this.customerUserMapper = customerUserMapper;
        this.customerSecurityMapper = customerSecurityMapper;
        this.auditLogMapper = auditLogMapper;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public ConsumePinStatus getStatus(Long customerId) {
        requireActiveCustomer(customerId);
        LocalDateTime now = LocalDateTime.now(clock);
        CustomerSecurity security = customerSecurityMapper.selectById(customerId);
        if (security == null || security.getConsumePinHash() == null) {
            return new ConsumePinStatus(false, false, null, null);
        }
        boolean locked = isLocked(security, now);
        return new ConsumePinStatus(
                true,
                locked,
                locked ? security.getLockedUntil() : null,
                security.getPinUpdatedTime()
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setInitialPin(Long customerId, String newPin, String clientIp) {
        requireStrongPin(newPin);
        requireActiveCustomer(customerId);
        LocalDateTime now = LocalDateTime.now(clock);
        CustomerSecurity security = customerSecurityMapper.selectByCustomerIdForUpdate(customerId);
        if (security != null && security.getConsumePinHash() != null) {
            throw new CustomException(ExceptionEnum.PLATFORM_CONSUME_PIN_ALREADY_SET);
        }

        String encodedPin = passwordEncoder.encode(newPin);
        if (security == null) {
            security = CustomerSecurity.builder()
                    .customerId(customerId)
                    .consumePinHash(encodedPin)
                    .failedCount(0)
                    .pinUpdatedTime(now)
                    .build();
            requireOneRow(customerSecurityMapper.insert(security));
        } else {
            security.setConsumePinHash(encodedPin);
            security.setFailedCount(0);
            security.setLockedUntil(null);
            security.setPinUpdatedTime(now);
            requireOneRow(customerSecurityMapper.updateById(security));
        }
        insertAudit(customerId, "CUSTOMER_CONSUME_PIN_SET", "客户首次设置消费密码", clientIp);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, noRollbackFor = CustomException.class)
    public void changePin(Long customerId, String currentPin, String newPin, String clientIp) {
        requireStrongPin(newPin);
        requireActiveCustomer(customerId);
        int maxFailedAttempts = validatedMaxFailedAttempts();
        Duration lockDuration = validatedLockDuration();
        LocalDateTime now = LocalDateTime.now(clock);
        CustomerSecurity security = requireConfiguredSecurity(customerId);
        requireNotLocked(security, now);

        if (!matchesPin(currentPin, security.getConsumePinHash())) {
            recordFailure(security, customerId, now, maxFailedAttempts, lockDuration, clientIp);
        }
        if (passwordEncoder.matches(newPin, security.getConsumePinHash())) {
            throw new CustomException(ExceptionEnum.PLATFORM_CONSUME_PIN_UNCHANGED);
        }

        security.setConsumePinHash(passwordEncoder.encode(newPin));
        security.setFailedCount(0);
        security.setLockedUntil(null);
        security.setPinUpdatedTime(now);
        requireOneRow(customerSecurityMapper.updateById(security));
        insertAudit(customerId, "CUSTOMER_CONSUME_PIN_CHANGED", "客户修改消费密码", clientIp);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, noRollbackFor = CustomException.class)
    public void verifyPin(Long customerId, String pin, String clientIp) {
        requireActiveCustomer(customerId);
        int maxFailedAttempts = validatedMaxFailedAttempts();
        Duration lockDuration = validatedLockDuration();
        LocalDateTime now = LocalDateTime.now(clock);
        CustomerSecurity security = requireConfiguredSecurity(customerId);
        requireNotLocked(security, now);

        if (!matchesPin(pin, security.getConsumePinHash())) {
            recordFailure(security, customerId, now, maxFailedAttempts, lockDuration, clientIp);
        }
        if (safeFailedCount(security) != 0 || security.getLockedUntil() != null) {
            security.setFailedCount(0);
            security.setLockedUntil(null);
            requireOneRow(customerSecurityMapper.updateById(security));
        }
    }

    private CustomerSecurity requireConfiguredSecurity(Long customerId) {
        CustomerSecurity security = customerSecurityMapper.selectByCustomerIdForUpdate(customerId);
        if (security == null || security.getConsumePinHash() == null) {
            throw new CustomException(ExceptionEnum.PLATFORM_CONSUME_PIN_NOT_SET);
        }
        return security;
    }

    private void requireActiveCustomer(Long customerId) {
        if (customerId == null || customerId <= 0) {
            throw new CustomException(ExceptionEnum.PLATFORM_INVALID_REQUEST);
        }
        CustomerUser customer = customerUserMapper.selectById(customerId);
        if (customer == null) {
            throw new CustomException(ExceptionEnum.PLATFORM_CUSTOMER_NOT_FOUND);
        }
        if (customer.getStatus() != CustomerStatus.ACTIVE) {
            throw new CustomException(ExceptionEnum.PLATFORM_CUSTOMER_DISABLED);
        }
    }

    private void requireStrongPin(String pin) {
        if (pin == null || !PIN_PATTERN.matcher(pin).matches() || WEAK_PINS.contains(pin)) {
            throw new CustomException(ExceptionEnum.PLATFORM_CONSUME_PIN_FORMAT_INVALID);
        }
    }

    private boolean matchesPin(String pin, String encodedPin) {
        return pin != null && PIN_PATTERN.matcher(pin).matches() && passwordEncoder.matches(pin, encodedPin);
    }

    private void requireNotLocked(CustomerSecurity security, LocalDateTime now) {
        if (isLocked(security, now)) {
            throw new CustomException(ExceptionEnum.PLATFORM_CONSUME_PIN_LOCKED);
        }
    }

    private boolean isLocked(CustomerSecurity security, LocalDateTime now) {
        return security.getLockedUntil() != null && security.getLockedUntil().isAfter(now);
    }

    private void recordFailure(
            CustomerSecurity security,
            Long customerId,
            LocalDateTime now,
            int maxFailedAttempts,
            Duration lockDuration,
            String clientIp
    ) {
        int failedCount = security.getLockedUntil() != null && !security.getLockedUntil().isAfter(now)
                ? 1
                : safeFailedCount(security) + 1;
        security.setFailedCount(failedCount);
        security.setLockedUntil(null);

        if (failedCount >= maxFailedAttempts) {
            security.setFailedCount(maxFailedAttempts);
            security.setLockedUntil(now.plus(lockDuration));
            requireOneRow(customerSecurityMapper.updateById(security));
            insertAudit(customerId, "CUSTOMER_CONSUME_PIN_LOCKED", "消费密码连续输错，账户临时锁定", clientIp);
            throw new CustomException(ExceptionEnum.PLATFORM_CONSUME_PIN_LOCKED);
        }

        requireOneRow(customerSecurityMapper.updateById(security));
        throw new CustomException(ExceptionEnum.PLATFORM_CONSUME_PIN_INCORRECT);
    }

    private int safeFailedCount(CustomerSecurity security) {
        return security.getFailedCount() == null ? 0 : Math.max(0, security.getFailedCount());
    }

    private int validatedMaxFailedAttempts() {
        int attempts = properties.getMaxFailedAttempts();
        if (attempts < 3 || attempts > 20) {
            throw new CustomException(ExceptionEnum.PLATFORM_CUSTOMER_PIN_CONFIGURATION_INVALID);
        }
        return attempts;
    }

    private Duration validatedLockDuration() {
        Duration duration = properties.getLockDuration();
        if (duration == null
                || duration.isZero()
                || duration.isNegative()
                || duration.compareTo(Duration.ofHours(24)) > 0) {
            throw new CustomException(ExceptionEnum.PLATFORM_CUSTOMER_PIN_CONFIGURATION_INVALID);
        }
        return duration;
    }

    private void insertAudit(Long customerId, String action, String remark, String clientIp) {
        AuditLog auditLog = AuditLog.builder()
                .actorType(AuditActorType.CUSTOMER)
                .actorId(customerId)
                .action(action)
                .resourceType("CUSTOMER_SECURITY")
                .resourceNo(String.valueOf(customerId))
                .remark(remark)
                .clientIp(normalizeClientIp(clientIp))
                .build();
        requireOneRow(auditLogMapper.insert(auditLog));
    }

    private void requireOneRow(int affectedRows) {
        if (affectedRows != 1) {
            throw new IllegalStateException("客户消费密码安全数据写入失败");
        }
    }

    private String normalizeClientIp(String clientIp) {
        if (clientIp == null || clientIp.isBlank()) {
            return null;
        }
        String normalized = clientIp.trim();
        return normalized.length() <= 45 ? normalized : normalized.substring(0, 45);
    }
}
