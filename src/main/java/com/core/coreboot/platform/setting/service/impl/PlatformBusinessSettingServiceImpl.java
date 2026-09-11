package com.core.coreboot.platform.setting.service.impl;

import com.core.coreboot.exception.CustomException;
import com.core.coreboot.exception.ExceptionEnum;
import com.core.coreboot.platform.audit.entity.AuditLog;
import com.core.coreboot.platform.audit.mapper.AuditLogMapper;
import com.core.coreboot.platform.common.enums.AuditActorType;
import com.core.coreboot.platform.common.enums.RoleCode;
import com.core.coreboot.platform.common.enums.SysUserStatus;
import com.core.coreboot.platform.common.model.PointMoneyPolicy;
import com.core.coreboot.platform.setting.entity.PlatformBusinessSetting;
import com.core.coreboot.platform.setting.mapper.PlatformBusinessSettingMapper;
import com.core.coreboot.platform.setting.model.CurrentBusinessPolicy;
import com.core.coreboot.platform.setting.model.PlatformBusinessSettingUpdateCommand;
import com.core.coreboot.platform.setting.model.PlatformBusinessSettingView;
import com.core.coreboot.platform.setting.service.PlatformBusinessSettingService;
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
@Transactional(readOnly = true)
public class PlatformBusinessSettingServiceImpl implements PlatformBusinessSettingService {
    private static final long SETTING_ID = 1L;
    private static final int POINTS_PER_YUAN = 1;
    private static final boolean BONUS_POINTS_ENABLED = false;
    private static final int MINIMUM_PENDING_TTL_MINUTES = 1;
    private static final int MAXIMUM_PENDING_TTL_MINUTES = 30;
    private static final int CHANGE_REASON_MAX_LENGTH = 500;

    private final PlatformBusinessSettingMapper settingMapper;
    private final SysUserMapper sysUserMapper;
    private final StaffAuthorityMapper staffAuthorityMapper;
    private final AuditLogMapper auditLogMapper;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Autowired
    public PlatformBusinessSettingServiceImpl(
            PlatformBusinessSettingMapper settingMapper,
            SysUserMapper sysUserMapper,
            StaffAuthorityMapper staffAuthorityMapper,
            AuditLogMapper auditLogMapper,
            ObjectMapper objectMapper
    ) {
        this(
                settingMapper,
                sysUserMapper,
                staffAuthorityMapper,
                auditLogMapper,
                objectMapper,
                Clock.systemDefaultZone()
        );
    }

    PlatformBusinessSettingServiceImpl(
            PlatformBusinessSettingMapper settingMapper,
            SysUserMapper sysUserMapper,
            StaffAuthorityMapper staffAuthorityMapper,
            AuditLogMapper auditLogMapper,
            ObjectMapper objectMapper,
            Clock clock
    ) {
        this.settingMapper = settingMapper;
        this.sysUserMapper = sysUserMapper;
        this.staffAuthorityMapper = staffAuthorityMapper;
        this.auditLogMapper = auditLogMapper;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Override
    public PlatformBusinessSettingView getCurrent(Long operatorId) {
        requireSuperAdmin(operatorId);
        PlatformBusinessSetting setting = requireValidSetting(settingMapper.selectById(SETTING_ID));
        return toView(setting, findUpdaterName(setting.getLastUpdatedBy()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PlatformBusinessSettingView update(PlatformBusinessSettingUpdateCommand command) {
        NormalizedUpdate normalized = normalize(command);
        SysUser operator = requireSuperAdmin(normalized.operatorId());
        PlatformBusinessSetting setting = requireValidSetting(
                settingMapper.selectByIdForUpdate(SETTING_ID)
        );
        if (!Objects.equals(setting.getVersion(), normalized.version())) {
            throw new CustomException(ExceptionEnum.PLATFORM_DATA_CONFLICT);
        }
        if (setting.getPlatformFeeRateBps() == normalized.platformFeeRateBps()
                && setting.getConsumptionPendingTtlMinutes()
                == normalized.consumptionPendingTtlMinutes()) {
            return toView(setting, findUpdaterName(setting.getLastUpdatedBy()));
        }

        Map<String, Object> before = snapshot(setting);
        LocalDateTime now = LocalDateTime.now(clock);
        int affectedRows = settingMapper.updateWithVersion(
                SETTING_ID,
                normalized.platformFeeRateBps(),
                normalized.consumptionPendingTtlMinutes(),
                normalized.version(),
                normalized.operatorId(),
                now
        );
        if (affectedRows != 1) {
            throw new CustomException(ExceptionEnum.PLATFORM_DATA_CONFLICT);
        }

        setting.setPlatformFeeRateBps(normalized.platformFeeRateBps());
        setting.setConsumptionPendingTtlMinutes(normalized.consumptionPendingTtlMinutes());
        setting.setVersion(normalized.version() + 1);
        setting.setLastUpdatedBy(normalized.operatorId());
        setting.setUpdateTime(now);
        requireOneRow(auditLogMapper.insert(buildAuditLog(
                normalized,
                before,
                snapshot(setting)
        )));
        return toView(setting, operator.getRealName());
    }

    @Override
    public CurrentBusinessPolicy currentPolicy() {
        PlatformBusinessSetting setting = requireValidSetting(settingMapper.selectById(SETTING_ID));
        return new CurrentBusinessPolicy(
                setting.getPlatformFeeRateBps(),
                setting.getConsumptionPendingTtlMinutes()
        );
    }

    private NormalizedUpdate normalize(PlatformBusinessSettingUpdateCommand command) {
        if (command == null
                || command.operatorId() == null
                || command.operatorId() <= 0
                || command.version() < 0
                || command.platformFeeRateBps() < 0
                || command.platformFeeRateBps() > PointMoneyPolicy.BASIS_POINTS_DIVISOR
                || command.consumptionPendingTtlMinutes() < MINIMUM_PENDING_TTL_MINUTES
                || command.consumptionPendingTtlMinutes() > MAXIMUM_PENDING_TTL_MINUTES) {
            throw new CustomException(ExceptionEnum.PLATFORM_INVALID_REQUEST);
        }
        String reason = normalizeRequired(command.changeReason());
        String clientIp = normalizeOptional(command.clientIp());
        if (reason == null
                || reason.length() > CHANGE_REASON_MAX_LENGTH
                || (clientIp != null && clientIp.length() > 45)) {
            throw new CustomException(ExceptionEnum.PLATFORM_INVALID_REQUEST);
        }
        return new NormalizedUpdate(
                command.platformFeeRateBps(),
                command.consumptionPendingTtlMinutes(),
                command.version(),
                reason,
                command.operatorId(),
                clientIp
        );
    }

    private SysUser requireSuperAdmin(Long operatorId) {
        if (operatorId == null || operatorId <= 0) {
            throw new CustomException(ExceptionEnum.PLATFORM_ADMIN_TOKEN_INVALID);
        }
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
        return operator;
    }

    private PlatformBusinessSetting requireValidSetting(PlatformBusinessSetting setting) {
        if (setting == null
                || !Objects.equals(setting.getId(), SETTING_ID)
                || setting.getPlatformFeeRateBps() == null
                || setting.getPlatformFeeRateBps() < 0
                || setting.getPlatformFeeRateBps() > PointMoneyPolicy.BASIS_POINTS_DIVISOR
                || setting.getConsumptionPendingTtlMinutes() == null
                || setting.getConsumptionPendingTtlMinutes() < MINIMUM_PENDING_TTL_MINUTES
                || setting.getConsumptionPendingTtlMinutes() > MAXIMUM_PENDING_TTL_MINUTES
                || setting.getVersion() == null
                || setting.getVersion() < 0) {
            throw new CustomException(ExceptionEnum.PLATFORM_BUSINESS_SETTING_INVALID);
        }
        return setting;
    }

    private String findUpdaterName(Long userId) {
        if (userId == null) {
            return null;
        }
        SysUser updater = sysUserMapper.selectById(userId);
        return updater == null ? null : updater.getRealName();
    }

    private PlatformBusinessSettingView toView(
            PlatformBusinessSetting setting,
            String updaterName
    ) {
        return new PlatformBusinessSettingView(
                POINTS_PER_YUAN,
                BONUS_POINTS_ENABLED,
                setting.getPlatformFeeRateBps(),
                setting.getConsumptionPendingTtlMinutes(),
                setting.getVersion(),
                setting.getLastUpdatedBy(),
                updaterName,
                setting.getUpdateTime()
        );
    }

    private Map<String, Object> snapshot(PlatformBusinessSetting setting) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("pointsPerYuan", POINTS_PER_YUAN);
        snapshot.put("bonusPointsEnabled", BONUS_POINTS_ENABLED);
        snapshot.put("platformFeeRateBps", setting.getPlatformFeeRateBps());
        snapshot.put(
                "consumptionPendingTtlMinutes",
                setting.getConsumptionPendingTtlMinutes()
        );
        snapshot.put("version", setting.getVersion());
        return snapshot;
    }

    private AuditLog buildAuditLog(
            NormalizedUpdate command,
            Map<String, Object> before,
            Map<String, Object> after
    ) {
        return AuditLog.builder()
                .actorType(AuditActorType.SYS_USER)
                .actorId(command.operatorId())
                .operatorRole(RoleCode.SUPER_ADMIN)
                .action("PLATFORM_BUSINESS_SETTING_UPDATED")
                .resourceType("PLATFORM_BUSINESS_SETTING")
                .resourceNo("GLOBAL")
                .beforeSnapshot(writeJson(before))
                .afterSnapshot(writeJson(after))
                .remark(command.changeReason())
                .clientIp(command.clientIp())
                .build();
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
            throw new CustomException(ExceptionEnum.PLATFORM_BUSINESS_SETTING_WRITE_FAILED);
        }
    }

    private String normalizeRequired(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private record NormalizedUpdate(
            int platformFeeRateBps,
            int consumptionPendingTtlMinutes,
            long version,
            String changeReason,
            Long operatorId,
            String clientIp
    ) {
    }
}
