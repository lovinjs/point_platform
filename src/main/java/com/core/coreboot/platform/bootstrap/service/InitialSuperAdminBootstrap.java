package com.core.coreboot.platform.bootstrap.service;

import com.core.coreboot.platform.audit.entity.AuditLog;
import com.core.coreboot.platform.audit.mapper.AuditLogMapper;
import com.core.coreboot.platform.bootstrap.config.AdminBootstrapProperties;
import com.core.coreboot.platform.common.enums.AuditActorType;
import com.core.coreboot.platform.common.enums.RoleCode;
import com.core.coreboot.platform.common.enums.SysUserStatus;
import com.core.coreboot.platform.staff.entity.SysRole;
import com.core.coreboot.platform.staff.entity.SysUser;
import com.core.coreboot.platform.staff.entity.SysUserRole;
import com.core.coreboot.platform.staff.mapper.SysRoleMapper;
import com.core.coreboot.platform.staff.mapper.SysUserMapper;
import com.core.coreboot.platform.staff.mapper.SysUserRoleMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.regex.Pattern;

@Component
@ConditionalOnProperty(prefix = "platform.bootstrap.admin", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class InitialSuperAdminBootstrap implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(InitialSuperAdminBootstrap.class);
    private static final Pattern USERNAME_PATTERN = Pattern.compile("[A-Za-z0-9._-]{3,64}");

    private final AdminBootstrapProperties properties;
    private final PasswordEncoder passwordEncoder;
    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final AuditLogMapper auditLogMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void run(ApplicationArguments args) {
        if (!properties.isEnabled()) {
            return;
        }
        if (sysRoleMapper.countAssignedSuperAdmins() > 0) {
            log.info("超级管理员已存在，跳过初始化；请关闭 PLATFORM_BOOTSTRAP_ADMIN_ENABLED");
            return;
        }

        BootstrapAdmin bootstrapAdmin = validateAndNormalize();
        if (sysUserMapper.selectByUsername(bootstrapAdmin.username()) != null) {
            throw new IllegalStateException("超级管理员初始化失败：用户名已经存在");
        }

        SysRole superAdminRole = sysRoleMapper.selectByRoleCode(RoleCode.SUPER_ADMIN.getCode());
        if (superAdminRole == null || !"ACTIVE".equals(superAdminRole.getStatus())) {
            throw new IllegalStateException("超级管理员初始化失败：SUPER_ADMIN 角色不存在或不可用");
        }

        SysUser user = SysUser.builder()
                .username(bootstrapAdmin.username())
                .phone(bootstrapAdmin.phone())
                .passwordHash(passwordEncoder.encode(bootstrapAdmin.password()))
                .realName(bootstrapAdmin.realName())
                .status(SysUserStatus.ACTIVE)
                .failedLoginCount(0)
                .tokenVersion(0)
                .passwordUpdatedTime(LocalDateTime.now())
                .build();
        requireOneRow(sysUserMapper.insert(user));
        requireOneRow(sysUserRoleMapper.insert(SysUserRole.builder()
                .userId(user.getId())
                .roleId(superAdminRole.getId())
                .build()));
        requireOneRow(auditLogMapper.insert(AuditLog.builder()
                .actorType(AuditActorType.SYSTEM)
                .operatorRole(RoleCode.SUPER_ADMIN)
                .action("INITIAL_SUPER_ADMIN_CREATED")
                .resourceType("SYS_USER")
                .resourceNo(String.valueOf(user.getId()))
                .remark("应用启动初始化首个超级管理员")
                .build()));

        log.warn("首个超级管理员已创建，用户名={}；请立即关闭 PLATFORM_BOOTSTRAP_ADMIN_ENABLED 并清除初始化密码环境变量",
                bootstrapAdmin.username());
    }

    private BootstrapAdmin validateAndNormalize() {
        String username = normalizeRequired(properties.getUsername());
        String password = properties.getPassword();
        String realName = normalizeRequired(properties.getRealName());
        String phone = normalizeOptional(properties.getPhone());

        if (username == null || !USERNAME_PATTERN.matcher(username).matches()) {
            throw invalid("用户名必须为3到64位字母、数字、点、下划线或横线");
        }
        if (!isStrongPassword(password)) {
            throw invalid("密码必须为12到128位，并包含大小写字母、数字和特殊字符");
        }
        if (realName == null || realName.length() > 64) {
            throw invalid("真实姓名不能为空且不能超过64位");
        }
        if (phone != null && phone.length() > 32) {
            throw invalid("手机号不能超过32位");
        }
        return new BootstrapAdmin(username, password, realName, phone);
    }

    private boolean isStrongPassword(String password) {
        if (password == null || password.length() < 12 || password.length() > 128) {
            return false;
        }
        boolean upper = false;
        boolean lower = false;
        boolean digit = false;
        boolean special = false;
        for (char character : password.toCharArray()) {
            upper |= Character.isUpperCase(character);
            lower |= Character.isLowerCase(character);
            digit |= Character.isDigit(character);
            special |= !Character.isLetterOrDigit(character);
        }
        return upper && lower && digit && special;
    }

    private void requireOneRow(int affectedRows) {
        if (affectedRows != 1) {
            throw new IllegalStateException("超级管理员初始化写入失败");
        }
    }

    private IllegalStateException invalid(String reason) {
        return new IllegalStateException("超级管理员初始化配置无效：" + reason);
    }

    private String normalizeRequired(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private record BootstrapAdmin(String username, String password, String realName, String phone) {
    }
}
