package com.core.coreboot.platform.merchant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.core.coreboot.exception.CustomException;
import com.core.coreboot.exception.ExceptionEnum;
import com.core.coreboot.platform.audit.entity.AuditLog;
import com.core.coreboot.platform.audit.mapper.AuditLogMapper;
import com.core.coreboot.platform.common.enums.AuditActorType;
import com.core.coreboot.platform.common.enums.MerchantStatus;
import com.core.coreboot.platform.common.enums.RoleCode;
import com.core.coreboot.platform.common.enums.SysUserStatus;
import com.core.coreboot.platform.common.model.PageResult;
import com.core.coreboot.platform.merchant.entity.Merchant;
import com.core.coreboot.platform.merchant.mapper.MerchantMapper;
import com.core.coreboot.platform.merchant.model.AdminMerchantCreateCommand;
import com.core.coreboot.platform.merchant.model.AdminMerchantManagementView;
import com.core.coreboot.platform.merchant.model.AdminMerchantStatusChangeCommand;
import com.core.coreboot.platform.merchant.model.AdminMerchantUpdateCommand;
import com.core.coreboot.platform.merchant.service.AdminMerchantManagementService;
import com.core.coreboot.platform.staff.entity.SysUser;
import com.core.coreboot.platform.staff.mapper.StaffAuthorityMapper;
import com.core.coreboot.platform.staff.mapper.SysUserMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

@Service
@Transactional(readOnly = true)
public class AdminMerchantManagementServiceImpl implements AdminMerchantManagementService {
    private static final int MAX_PAGE_SIZE = 100;
    private static final Pattern MERCHANT_CODE_PATTERN = Pattern.compile("[A-Za-z0-9_-]+");
    private static final Pattern CREDIT_CODE_PATTERN = Pattern.compile("[0-9A-Z]{18}");
    private static final Pattern PHONE_PATTERN = Pattern.compile("[0-9+()\\-\\s]{5,32}");

    private final MerchantMapper merchantMapper;
    private final SysUserMapper sysUserMapper;
    private final StaffAuthorityMapper staffAuthorityMapper;
    private final AuditLogMapper auditLogMapper;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Autowired
    public AdminMerchantManagementServiceImpl(
            MerchantMapper merchantMapper,
            SysUserMapper sysUserMapper,
            StaffAuthorityMapper staffAuthorityMapper,
            AuditLogMapper auditLogMapper,
            ObjectMapper objectMapper
    ) {
        this(
                merchantMapper,
                sysUserMapper,
                staffAuthorityMapper,
                auditLogMapper,
                objectMapper,
                Clock.systemDefaultZone()
        );
    }

    AdminMerchantManagementServiceImpl(
            MerchantMapper merchantMapper,
            SysUserMapper sysUserMapper,
            StaffAuthorityMapper staffAuthorityMapper,
            AuditLogMapper auditLogMapper,
            ObjectMapper objectMapper,
            Clock clock
    ) {
        this.merchantMapper = merchantMapper;
        this.sysUserMapper = sysUserMapper;
        this.staffAuthorityMapper = staffAuthorityMapper;
        this.auditLogMapper = auditLogMapper;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Override
    public PageResult<AdminMerchantManagementView> list(
            Long operatorId,
            int pageNum,
            int pageSize,
            MerchantStatus status,
            String keyword
    ) {
        requireSuperAdmin(operatorId);
        validatePage(pageNum, pageSize);
        String normalizedKeyword = normalizeOptional(keyword);
        if (normalizedKeyword != null && normalizedKeyword.length() > 200) {
            throw new CustomException(ExceptionEnum.PLATFORM_INVALID_REQUEST);
        }

        LambdaQueryWrapper<Merchant> wrapper = Wrappers.lambdaQuery(Merchant.class)
                .eq(status != null, Merchant::getStatus, status)
                .and(normalizedKeyword != null, condition -> condition
                        .like(Merchant::getMerchantCode, normalizedKeyword)
                        .or()
                        .like(Merchant::getLegalName, normalizedKeyword)
                        .or()
                        .like(Merchant::getBusinessName, normalizedKeyword)
                        .or()
                        .like(Merchant::getContactName, normalizedKeyword)
                        .or()
                        .like(Merchant::getContactPhone, normalizedKeyword))
                .orderByDesc(Merchant::getCreateTime)
                .orderByDesc(Merchant::getId);
        IPage<Merchant> page = merchantMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        List<AdminMerchantManagementView> items = page.getRecords().stream()
                .map(this::toView)
                .toList();
        return new PageResult<>(
                page.getCurrent(),
                page.getSize(),
                page.getTotal(),
                page.getPages(),
                page.getCurrent() < page.getPages(),
                items
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminMerchantManagementView create(AdminMerchantCreateCommand command) {
        NormalizedCreate normalized = normalize(command);
        requireSuperAdmin(normalized.operatorId());
        requireCodeAvailable(normalized.merchantCode(), null);
        requireCreditCodeAvailable(normalized.unifiedSocialCreditCode(), null);

        LocalDateTime now = LocalDateTime.now(clock);
        Merchant merchant = Merchant.builder()
                .merchantCode(normalized.merchantCode())
                .legalName(normalized.legalName())
                .businessName(normalized.businessName())
                .unifiedSocialCreditCode(normalized.unifiedSocialCreditCode())
                .contactName(normalized.contactName())
                .contactPhone(normalized.contactPhone())
                .agreementNo(normalized.agreementNo())
                .status(MerchantStatus.ACTIVE)
                .createTime(now)
                .updateTime(now)
                .build();
        try {
            requireOneRow(merchantMapper.insert(merchant));
        } catch (DuplicateKeyException ex) {
            throw new CustomException(ExceptionEnum.PLATFORM_DATA_CONFLICT);
        }
        requireOneRow(auditLogMapper.insert(buildAudit(
                merchant,
                normalized.operatorId(),
                normalized.clientIp(),
                "MERCHANT_CREATED",
                null,
                snapshot(merchant),
                "创建合作商户"
        )));
        return toView(merchant);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminMerchantManagementView update(AdminMerchantUpdateCommand command) {
        NormalizedUpdate normalized = normalize(command);
        requireSuperAdmin(normalized.operatorId());
        Merchant merchant = requireMerchantForUpdate(normalized.merchantId());
        requireCreditCodeAvailable(normalized.unifiedSocialCreditCode(), merchant.getId());
        Map<String, Object> before = snapshot(merchant);

        merchant.setLegalName(normalized.legalName());
        merchant.setBusinessName(normalized.businessName());
        merchant.setUnifiedSocialCreditCode(normalized.unifiedSocialCreditCode());
        merchant.setContactName(normalized.contactName());
        merchant.setContactPhone(normalized.contactPhone());
        merchant.setAgreementNo(normalized.agreementNo());
        merchant.setUpdateTime(LocalDateTime.now(clock));
        try {
            requireOneRow(merchantMapper.updateById(merchant));
        } catch (DuplicateKeyException ex) {
            throw new CustomException(ExceptionEnum.PLATFORM_DATA_CONFLICT);
        }
        requireOneRow(auditLogMapper.insert(buildAudit(
                merchant,
                normalized.operatorId(),
                normalized.clientIp(),
                "MERCHANT_UPDATED",
                before,
                snapshot(merchant),
                "修改合作商户资料"
        )));
        return toView(merchant);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminMerchantManagementView changeStatus(AdminMerchantStatusChangeCommand command) {
        NormalizedStatusChange normalized = normalize(command);
        requireSuperAdmin(normalized.operatorId());
        Merchant merchant = requireMerchantForUpdate(normalized.merchantId());
        if (merchant.getStatus() == MerchantStatus.TERMINATED) {
            throw new CustomException(ExceptionEnum.PLATFORM_MERCHANT_STATUS_INVALID);
        }
        if (merchant.getStatus() == normalized.status()) {
            return toView(merchant);
        }

        Map<String, Object> before = snapshot(merchant);
        merchant.setStatus(normalized.status());
        merchant.setUpdateTime(LocalDateTime.now(clock));
        requireOneRow(merchantMapper.updateById(merchant));
        boolean enabled = normalized.status() == MerchantStatus.ACTIVE;
        requireOneRow(auditLogMapper.insert(buildAudit(
                merchant,
                normalized.operatorId(),
                normalized.clientIp(),
                enabled ? "MERCHANT_ENABLED" : "MERCHANT_SUSPENDED",
                before,
                snapshot(merchant),
                enabled ? "启用合作商户" : "停用合作商户"
        )));
        return toView(merchant);
    }

    private NormalizedCreate normalize(AdminMerchantCreateCommand command) {
        if (command == null || command.operatorId() == null || command.operatorId() <= 0) {
            throw new CustomException(ExceptionEnum.PLATFORM_INVALID_REQUEST);
        }
        String merchantCode = normalizeRequired(command.merchantCode()).toUpperCase(Locale.ROOT);
        String legalName = normalizeRequired(command.legalName());
        String businessName = normalizeRequired(command.businessName());
        String creditCode = normalizeUpperOptional(command.unifiedSocialCreditCode());
        String contactName = normalizeRequired(command.contactName());
        String contactPhone = normalizeRequired(command.contactPhone());
        String agreementNo = normalizeOptional(command.agreementNo());
        String clientIp = normalizeOptional(command.clientIp());
        validateFields(
                merchantCode,
                legalName,
                businessName,
                creditCode,
                contactName,
                contactPhone,
                agreementNo,
                clientIp
        );
        return new NormalizedCreate(
                merchantCode,
                legalName,
                businessName,
                creditCode,
                contactName,
                contactPhone,
                agreementNo,
                command.operatorId(),
                clientIp
        );
    }

    private NormalizedUpdate normalize(AdminMerchantUpdateCommand command) {
        if (command == null || command.merchantId() == null || command.merchantId() <= 0
                || command.operatorId() == null || command.operatorId() <= 0) {
            throw new CustomException(ExceptionEnum.PLATFORM_INVALID_REQUEST);
        }
        String legalName = normalizeRequired(command.legalName());
        String businessName = normalizeRequired(command.businessName());
        String creditCode = normalizeUpperOptional(command.unifiedSocialCreditCode());
        String contactName = normalizeRequired(command.contactName());
        String contactPhone = normalizeRequired(command.contactPhone());
        String agreementNo = normalizeOptional(command.agreementNo());
        String clientIp = normalizeOptional(command.clientIp());
        validateFields(
                null,
                legalName,
                businessName,
                creditCode,
                contactName,
                contactPhone,
                agreementNo,
                clientIp
        );
        return new NormalizedUpdate(
                command.merchantId(),
                legalName,
                businessName,
                creditCode,
                contactName,
                contactPhone,
                agreementNo,
                command.operatorId(),
                clientIp
        );
    }

    private NormalizedStatusChange normalize(AdminMerchantStatusChangeCommand command) {
        if (command == null || command.merchantId() == null || command.merchantId() <= 0
                || command.operatorId() == null || command.operatorId() <= 0
                || (command.status() != MerchantStatus.ACTIVE
                && command.status() != MerchantStatus.SUSPENDED)) {
            throw new CustomException(ExceptionEnum.PLATFORM_MERCHANT_STATUS_INVALID);
        }
        String clientIp = normalizeOptional(command.clientIp());
        if (clientIp != null && clientIp.length() > 45) {
            throw new CustomException(ExceptionEnum.PLATFORM_INVALID_REQUEST);
        }
        return new NormalizedStatusChange(
                command.merchantId(), command.status(), command.operatorId(), clientIp
        );
    }

    private void validateFields(
            String merchantCode,
            String legalName,
            String businessName,
            String creditCode,
            String contactName,
            String contactPhone,
            String agreementNo,
            String clientIp
    ) {
        if ((merchantCode != null && (merchantCode.length() > 64
                || !MERCHANT_CODE_PATTERN.matcher(merchantCode).matches()))
                || legalName.length() > 200
                || businessName.length() > 200
                || (creditCode != null && !CREDIT_CODE_PATTERN.matcher(creditCode).matches())
                || contactName.length() > 64
                || !PHONE_PATTERN.matcher(contactPhone).matches()
                || (agreementNo != null && agreementNo.length() > 100)
                || (clientIp != null && clientIp.length() > 45)) {
            throw new CustomException(ExceptionEnum.PLATFORM_INVALID_REQUEST);
        }
    }

    private void validatePage(int pageNum, int pageSize) {
        if (pageNum < 1 || pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw new CustomException(ExceptionEnum.PLATFORM_INVALID_REQUEST);
        }
    }

    private void requireCodeAvailable(String merchantCode, Long excludedMerchantId) {
        long count = merchantMapper.selectCount(Wrappers.lambdaQuery(Merchant.class)
                .eq(Merchant::getMerchantCode, merchantCode)
                .ne(excludedMerchantId != null, Merchant::getId, excludedMerchantId));
        if (count > 0) {
            throw new CustomException(ExceptionEnum.PLATFORM_MERCHANT_CODE_USED);
        }
    }

    private void requireCreditCodeAvailable(String creditCode, Long excludedMerchantId) {
        if (creditCode == null) {
            return;
        }
        long count = merchantMapper.selectCount(Wrappers.lambdaQuery(Merchant.class)
                .eq(Merchant::getUnifiedSocialCreditCode, creditCode)
                .ne(excludedMerchantId != null, Merchant::getId, excludedMerchantId));
        if (count > 0) {
            throw new CustomException(ExceptionEnum.PLATFORM_MERCHANT_CREDIT_CODE_USED);
        }
    }

    private void requireSuperAdmin(Long operatorId) {
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
    }

    private Merchant requireMerchantForUpdate(Long merchantId) {
        Merchant merchant = merchantMapper.selectByIdForUpdate(merchantId);
        if (merchant == null) {
            throw new CustomException(ExceptionEnum.PLATFORM_MERCHANT_NOT_FOUND);
        }
        return merchant;
    }

    private AdminMerchantManagementView toView(Merchant merchant) {
        return new AdminMerchantManagementView(
                merchant.getId(),
                merchant.getMerchantCode(),
                merchant.getLegalName(),
                merchant.getBusinessName(),
                merchant.getUnifiedSocialCreditCode(),
                merchant.getContactName(),
                merchant.getContactPhone(),
                merchant.getAgreementNo(),
                merchant.getStatus(),
                merchant.getCreateTime(),
                merchant.getUpdateTime()
        );
    }

    private Map<String, Object> snapshot(Merchant merchant) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("merchantId", merchant.getId());
        snapshot.put("merchantCode", merchant.getMerchantCode());
        snapshot.put("legalName", merchant.getLegalName());
        snapshot.put("businessName", merchant.getBusinessName());
        snapshot.put("unifiedSocialCreditCode", merchant.getUnifiedSocialCreditCode());
        snapshot.put("contactName", merchant.getContactName());
        snapshot.put("contactPhone", merchant.getContactPhone());
        snapshot.put("agreementNo", merchant.getAgreementNo());
        snapshot.put("status", merchant.getStatus() == null ? null : merchant.getStatus().getCode());
        return snapshot;
    }

    private AuditLog buildAudit(
            Merchant merchant,
            Long operatorId,
            String clientIp,
            String action,
            Map<String, Object> before,
            Map<String, Object> after,
            String remark
    ) {
        return AuditLog.builder()
                .actorType(AuditActorType.SYS_USER)
                .actorId(operatorId)
                .operatorRole(RoleCode.SUPER_ADMIN)
                .action(action)
                .resourceType("MERCHANT")
                .resourceNo(merchant.getMerchantCode())
                .beforeSnapshot(writeJson(before))
                .afterSnapshot(writeJson(after))
                .remark(remark)
                .clientIp(clientIp)
                .build();
    }

    private String writeJson(Map<String, Object> value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("合作商户审计快照序列化失败", ex);
        }
    }

    private String normalizeRequired(String value) {
        String normalized = normalizeOptional(value);
        if (normalized == null) {
            throw new CustomException(ExceptionEnum.PLATFORM_INVALID_REQUEST);
        }
        return normalized;
    }

    private String normalizeUpperOptional(String value) {
        String normalized = normalizeOptional(value);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private void requireOneRow(int affectedRows) {
        if (affectedRows != 1) {
            throw new CustomException(ExceptionEnum.PLATFORM_MERCHANT_MANAGEMENT_WRITE_FAILED);
        }
    }

    private record NormalizedCreate(
            String merchantCode,
            String legalName,
            String businessName,
            String unifiedSocialCreditCode,
            String contactName,
            String contactPhone,
            String agreementNo,
            Long operatorId,
            String clientIp
    ) {
    }

    private record NormalizedUpdate(
            Long merchantId,
            String legalName,
            String businessName,
            String unifiedSocialCreditCode,
            String contactName,
            String contactPhone,
            String agreementNo,
            Long operatorId,
            String clientIp
    ) {
    }

    private record NormalizedStatusChange(
            Long merchantId,
            MerchantStatus status,
            Long operatorId,
            String clientIp
    ) {
    }
}
