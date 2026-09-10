package com.core.coreboot.platform.auth.service.impl;

import com.core.coreboot.exception.CustomException;
import com.core.coreboot.exception.ExceptionEnum;
import com.core.coreboot.platform.audit.entity.AuditLog;
import com.core.coreboot.platform.audit.mapper.AuditLogMapper;
import com.core.coreboot.platform.auth.service.AdminPasswordService;
import com.core.coreboot.platform.common.enums.AuditActorType;
import com.core.coreboot.platform.common.enums.RoleCode;
import com.core.coreboot.platform.common.enums.SysUserStatus;
import com.core.coreboot.platform.staff.entity.SysUser;
import com.core.coreboot.platform.staff.mapper.SysUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Set;

@Service
public class AdminPasswordServiceImpl implements AdminPasswordService {
    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogMapper auditLogMapper;
    private final Clock clock;

    @Autowired
    public AdminPasswordServiceImpl(
            SysUserMapper sysUserMapper,
            PasswordEncoder passwordEncoder,
            AuditLogMapper auditLogMapper
    ) {
        this(sysUserMapper, passwordEncoder, auditLogMapper, Clock.systemDefaultZone());
    }

    AdminPasswordServiceImpl(
            SysUserMapper sysUserMapper,
            PasswordEncoder passwordEncoder,
            AuditLogMapper auditLogMapper,
            Clock clock
    ) {
        this.sysUserMapper = sysUserMapper;
        this.passwordEncoder = passwordEncoder;
        this.auditLogMapper = auditLogMapper;
        this.clock = clock;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeOwnPassword(
            Long userId,
            Set<RoleCode> roles,
            String currentPassword,
            String newPassword,
            String clientIp
    ) {
        validateRequest(userId, currentPassword, newPassword);
        RoleCode actorRole = resolveActorRole(roles);
        SysUser user = sysUserMapper.selectByIdForUpdate(userId);
        if (user == null) {
            throw new CustomException(ExceptionEnum.PLATFORM_OPERATOR_NOT_FOUND);
        }
        if (user.getStatus() != SysUserStatus.ACTIVE) {
            throw new CustomException(ExceptionEnum.PLATFORM_OPERATOR_DISABLED);
        }
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new CustomException(ExceptionEnum.PLATFORM_ADMIN_CURRENT_PASSWORD_INCORRECT);
        }
        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            throw new CustomException(ExceptionEnum.PLATFORM_STAFF_PASSWORD_UNCHANGED);
        }

        LocalDateTime now = LocalDateTime.now(clock);
        if (sysUserMapper.resetStaffPassword(userId, passwordEncoder.encode(newPassword), now) != 1) {
            throw new CustomException(ExceptionEnum.PLATFORM_STAFF_MANAGEMENT_WRITE_FAILED);
        }
        AuditLog auditLog = AuditLog.builder()
                .actorType(AuditActorType.SYS_USER)
                .actorId(userId)
                .operatorRole(actorRole)
                .action("ADMIN_PASSWORD_CHANGED")
                .resourceType("SYS_USER")
                .resourceNo(user.getUsername())
                .remark("后台账号修改自己的登录密码，全部原登录凭证已失效")
                .clientIp(normalizeClientIp(clientIp))
                .build();
        if (auditLogMapper.insert(auditLog) != 1) {
            throw new CustomException(ExceptionEnum.PLATFORM_STAFF_MANAGEMENT_WRITE_FAILED);
        }
    }

    private void validateRequest(Long userId, String currentPassword, String newPassword) {
        if (userId == null || userId <= 0 || currentPassword == null
                || currentPassword.isBlank() || currentPassword.length() > 128) {
            throw new CustomException(ExceptionEnum.PLATFORM_INVALID_REQUEST);
        }
        if (newPassword == null || newPassword.length() < 12 || newPassword.length() > 128) {
            throw new CustomException(ExceptionEnum.PLATFORM_STAFF_PASSWORD_INVALID);
        }
        boolean upper = false;
        boolean lower = false;
        boolean digit = false;
        boolean special = false;
        for (char character : newPassword.toCharArray()) {
            upper |= Character.isUpperCase(character);
            lower |= Character.isLowerCase(character);
            digit |= Character.isDigit(character);
            special |= !Character.isLetterOrDigit(character);
        }
        if (!upper || !lower || !digit || !special) {
            throw new CustomException(ExceptionEnum.PLATFORM_STAFF_PASSWORD_INVALID);
        }
    }

    private RoleCode resolveActorRole(Set<RoleCode> roles) {
        if (roles != null) {
            if (roles.contains(RoleCode.SUPER_ADMIN)) {
                return RoleCode.SUPER_ADMIN;
            }
            if (roles.contains(RoleCode.STORE_MANAGER)) {
                return RoleCode.STORE_MANAGER;
            }
            if (roles.contains(RoleCode.CLERK)) {
                return RoleCode.CLERK;
            }
        }
        throw new CustomException(ExceptionEnum.PLATFORM_ADMIN_AUTHORITY_INVALID);
    }

    private String normalizeClientIp(String clientIp) {
        if (clientIp == null || clientIp.isBlank()) {
            return null;
        }
        String normalized = clientIp.trim();
        return normalized.length() <= 45 ? normalized : normalized.substring(0, 45);
    }
}
