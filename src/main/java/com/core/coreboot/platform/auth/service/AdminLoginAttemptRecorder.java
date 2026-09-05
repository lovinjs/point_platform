package com.core.coreboot.platform.auth.service;

import com.core.coreboot.exception.CustomException;
import com.core.coreboot.exception.ExceptionEnum;
import com.core.coreboot.platform.audit.entity.AuditLog;
import com.core.coreboot.platform.audit.mapper.AuditLogMapper;
import com.core.coreboot.platform.auth.config.AdminSecurityProperties;
import com.core.coreboot.platform.auth.security.AdminUserPrincipal;
import com.core.coreboot.platform.common.enums.AuditActorType;
import com.core.coreboot.platform.common.enums.RoleCode;
import com.core.coreboot.platform.staff.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;

@Service
@RequiredArgsConstructor
public class AdminLoginAttemptRecorder {
    private final AdminSecurityProperties properties;
    private final SysUserMapper sysUserMapper;
    private final AuditLogMapper auditLogMapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(String username) {
        if (username == null || username.isBlank()) {
            return;
        }
        int maxFailedAttempts = validatedMaxFailedAttempts();
        Duration lockDuration = validatedLockDuration();
        sysUserMapper.recordLoginFailure(
                username.trim(),
                maxFailedAttempts,
                LocalDateTime.now().plus(lockDuration)
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void recordSuccess(AdminUserPrincipal principal, String clientIp) {
        if (sysUserMapper.recordLoginSuccess(principal.getUserId(), LocalDateTime.now()) != 1) {
            throw new CustomException(ExceptionEnum.PLATFORM_ADMIN_LOGIN_FAILED);
        }
        if (auditLogMapper.insert(buildLoginAudit(principal, clientIp)) != 1) {
            throw new CustomException(ExceptionEnum.PLATFORM_ADMIN_LOGIN_FAILED);
        }
    }

    private AuditLog buildLoginAudit(AdminUserPrincipal principal, String clientIp) {
        RoleCode effectiveRole = principal.getRoles().stream()
                .min(Comparator.comparingInt(this::rolePriority))
                .orElse(null);
        return AuditLog.builder()
                .actorType(AuditActorType.SYS_USER)
                .actorId(principal.getUserId())
                .operatorRole(effectiveRole)
                .action("ADMIN_LOGIN_SUCCEEDED")
                .resourceType("SYS_USER")
                .resourceNo(String.valueOf(principal.getUserId()))
                .clientIp(normalizeClientIp(clientIp))
                .build();
    }

    private int rolePriority(RoleCode roleCode) {
        return switch (roleCode) {
            case SUPER_ADMIN -> 1;
            case STORE_MANAGER -> 2;
            case CLERK -> 3;
        };
    }

    private int validatedMaxFailedAttempts() {
        int attempts = properties.getMaxFailedAttempts();
        if (attempts < 3 || attempts > 20) {
            throw new CustomException(ExceptionEnum.PLATFORM_ADMIN_SECURITY_NOT_CONFIGURED);
        }
        return attempts;
    }

    private Duration validatedLockDuration() {
        Duration duration = properties.getLockDuration();
        if (duration == null
                || duration.isNegative()
                || duration.isZero()
                || duration.compareTo(Duration.ofHours(24)) > 0) {
            throw new CustomException(ExceptionEnum.PLATFORM_ADMIN_SECURITY_NOT_CONFIGURED);
        }
        return duration;
    }

    private String normalizeClientIp(String clientIp) {
        if (clientIp == null || clientIp.isBlank()) {
            return null;
        }
        String normalized = clientIp.trim();
        return normalized.length() <= 45 ? normalized : normalized.substring(0, 45);
    }
}
