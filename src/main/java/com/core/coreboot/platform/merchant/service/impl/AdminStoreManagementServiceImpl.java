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
import com.core.coreboot.platform.common.enums.StoreStatus;
import com.core.coreboot.platform.common.enums.SysUserStatus;
import com.core.coreboot.platform.common.model.PageResult;
import com.core.coreboot.platform.merchant.entity.Merchant;
import com.core.coreboot.platform.merchant.entity.Store;
import com.core.coreboot.platform.merchant.mapper.MerchantMapper;
import com.core.coreboot.platform.merchant.mapper.StoreMapper;
import com.core.coreboot.platform.merchant.model.AdminMerchantOptionView;
import com.core.coreboot.platform.merchant.model.AdminStoreCreateCommand;
import com.core.coreboot.platform.merchant.model.AdminStoreManagementView;
import com.core.coreboot.platform.merchant.model.AdminStoreStatusChangeCommand;
import com.core.coreboot.platform.merchant.model.AdminStoreUpdateCommand;
import com.core.coreboot.platform.merchant.service.AdminStoreManagementService;
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
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class AdminStoreManagementServiceImpl implements AdminStoreManagementService {
    private static final int MAX_PAGE_SIZE = 100;
    private static final Pattern STORE_CODE_PATTERN = Pattern.compile("[A-Za-z0-9_-]+");
    private static final Pattern PHONE_PATTERN = Pattern.compile("[0-9+()\\-\\s]{5,32}");

    private final StoreMapper storeMapper;
    private final MerchantMapper merchantMapper;
    private final SysUserMapper sysUserMapper;
    private final StaffAuthorityMapper staffAuthorityMapper;
    private final AuditLogMapper auditLogMapper;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Autowired
    public AdminStoreManagementServiceImpl(
            StoreMapper storeMapper,
            MerchantMapper merchantMapper,
            SysUserMapper sysUserMapper,
            StaffAuthorityMapper staffAuthorityMapper,
            AuditLogMapper auditLogMapper,
            ObjectMapper objectMapper
    ) {
        this(
                storeMapper,
                merchantMapper,
                sysUserMapper,
                staffAuthorityMapper,
                auditLogMapper,
                objectMapper,
                Clock.systemDefaultZone()
        );
    }

    AdminStoreManagementServiceImpl(
            StoreMapper storeMapper,
            MerchantMapper merchantMapper,
            SysUserMapper sysUserMapper,
            StaffAuthorityMapper staffAuthorityMapper,
            AuditLogMapper auditLogMapper,
            ObjectMapper objectMapper,
            Clock clock
    ) {
        this.storeMapper = storeMapper;
        this.merchantMapper = merchantMapper;
        this.sysUserMapper = sysUserMapper;
        this.staffAuthorityMapper = staffAuthorityMapper;
        this.auditLogMapper = auditLogMapper;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Override
    public PageResult<AdminStoreManagementView> list(
            Long operatorId,
            int pageNum,
            int pageSize,
            Long merchantId,
            StoreStatus status,
            String keyword
    ) {
        requireSuperAdmin(operatorId);
        validatePage(pageNum, pageSize);
        if (merchantId != null && merchantId <= 0) {
            throw new CustomException(ExceptionEnum.PLATFORM_INVALID_REQUEST);
        }
        String normalizedKeyword = normalizeOptional(keyword);
        if (normalizedKeyword != null && normalizedKeyword.length() > 200) {
            throw new CustomException(ExceptionEnum.PLATFORM_INVALID_REQUEST);
        }

        LambdaQueryWrapper<Store> wrapper = Wrappers.lambdaQuery(Store.class)
                .eq(merchantId != null, Store::getMerchantId, merchantId)
                .eq(status != null, Store::getStatus, status)
                .and(normalizedKeyword != null, condition -> condition
                        .like(Store::getStoreCode, normalizedKeyword)
                        .or()
                        .like(Store::getStoreName, normalizedKeyword))
                .orderByDesc(Store::getCreateTime)
                .orderByDesc(Store::getId);
        IPage<Store> page = storeMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        Map<Long, Merchant> merchants = loadMerchants(page.getRecords());
        List<AdminStoreManagementView> items = page.getRecords().stream()
                .map(store -> toView(store, merchants.get(store.getMerchantId())))
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
    public List<AdminMerchantOptionView> listActiveMerchantOptions(Long operatorId) {
        requireSuperAdmin(operatorId);
        return merchantMapper.selectList(Wrappers.lambdaQuery(Merchant.class)
                        .eq(Merchant::getStatus, MerchantStatus.ACTIVE)
                        .orderByAsc(Merchant::getBusinessName)
                        .orderByAsc(Merchant::getId))
                .stream()
                .map(merchant -> new AdminMerchantOptionView(
                        merchant.getId(),
                        merchant.getMerchantCode(),
                        merchant.getBusinessName()
                ))
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminStoreManagementView create(AdminStoreCreateCommand command) {
        NormalizedCreate normalized = normalize(command);
        requireSuperAdmin(normalized.operatorId());
        Merchant merchant = requireActiveMerchant(normalized.merchantId());
        if (storeMapper.selectCount(Wrappers.lambdaQuery(Store.class)
                .eq(Store::getStoreCode, normalized.storeCode())) > 0) {
            throw new CustomException(ExceptionEnum.PLATFORM_STORE_CODE_USED);
        }

        LocalDateTime now = LocalDateTime.now(clock);
        Store store = Store.builder()
                .merchantId(normalized.merchantId())
                .storeCode(normalized.storeCode())
                .storeName(normalized.storeName())
                .address(normalized.address())
                .contactPhone(normalized.contactPhone())
                .status(StoreStatus.ACTIVE)
                .createTime(now)
                .updateTime(now)
                .build();
        try {
            requireOneRow(storeMapper.insert(store));
        } catch (DuplicateKeyException ex) {
            throw new CustomException(ExceptionEnum.PLATFORM_STORE_CODE_USED);
        }
        requireOneRow(auditLogMapper.insert(buildAudit(
                store,
                normalized.operatorId(),
                normalized.clientIp(),
                "STORE_CREATED",
                null,
                snapshot(store),
                "创建门店"
        )));
        return toView(store, merchant);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminStoreManagementView update(AdminStoreUpdateCommand command) {
        NormalizedUpdate normalized = normalize(command);
        requireSuperAdmin(normalized.operatorId());
        Store store = requireStoreForUpdate(normalized.storeId());
        Map<String, Object> before = snapshot(store);

        store.setStoreName(normalized.storeName());
        store.setAddress(normalized.address());
        store.setContactPhone(normalized.contactPhone());
        store.setUpdateTime(LocalDateTime.now(clock));
        requireOneRow(storeMapper.updateById(store));
        requireOneRow(auditLogMapper.insert(buildAudit(
                store,
                normalized.operatorId(),
                normalized.clientIp(),
                "STORE_UPDATED",
                before,
                snapshot(store),
                "修改门店资料"
        )));
        return toView(store, requireMerchant(store.getMerchantId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminStoreManagementView changeStatus(AdminStoreStatusChangeCommand command) {
        NormalizedStatusChange normalized = normalize(command);
        requireSuperAdmin(normalized.operatorId());
        Store store = requireStoreForUpdate(normalized.storeId());
        if (store.getStatus() == StoreStatus.CLOSED) {
            throw new CustomException(ExceptionEnum.PLATFORM_STORE_STATUS_INVALID);
        }
        Merchant merchant = requireMerchant(store.getMerchantId());
        if (store.getStatus() == normalized.status()) {
            return toView(store, merchant);
        }
        if (normalized.status() == StoreStatus.ACTIVE && merchant.getStatus() != MerchantStatus.ACTIVE) {
            throw new CustomException(ExceptionEnum.PLATFORM_MERCHANT_UNAVAILABLE);
        }

        Map<String, Object> before = snapshot(store);
        store.setStatus(normalized.status());
        store.setUpdateTime(LocalDateTime.now(clock));
        requireOneRow(storeMapper.updateById(store));
        boolean enabled = normalized.status() == StoreStatus.ACTIVE;
        requireOneRow(auditLogMapper.insert(buildAudit(
                store,
                normalized.operatorId(),
                normalized.clientIp(),
                enabled ? "STORE_ENABLED" : "STORE_SUSPENDED",
                before,
                snapshot(store),
                enabled ? "启用门店" : "停用门店"
        )));
        return toView(store, merchant);
    }

    private NormalizedCreate normalize(AdminStoreCreateCommand command) {
        if (command == null || command.merchantId() == null || command.merchantId() <= 0
                || command.operatorId() == null || command.operatorId() <= 0) {
            throw new CustomException(ExceptionEnum.PLATFORM_INVALID_REQUEST);
        }
        String storeCode = normalizeRequired(command.storeCode());
        String storeName = normalizeRequired(command.storeName());
        String address = normalizeRequired(command.address());
        String contactPhone = normalizeRequired(command.contactPhone());
        String clientIp = normalizeOptional(command.clientIp());
        validateStoreFields(storeCode, storeName, address, contactPhone, clientIp);
        return new NormalizedCreate(
                command.merchantId(),
                storeCode.toUpperCase(Locale.ROOT),
                storeName,
                address,
                contactPhone,
                command.operatorId(),
                clientIp
        );
    }

    private NormalizedUpdate normalize(AdminStoreUpdateCommand command) {
        if (command == null || command.storeId() == null || command.storeId() <= 0
                || command.operatorId() == null || command.operatorId() <= 0) {
            throw new CustomException(ExceptionEnum.PLATFORM_INVALID_REQUEST);
        }
        String storeName = normalizeRequired(command.storeName());
        String address = normalizeRequired(command.address());
        String contactPhone = normalizeRequired(command.contactPhone());
        String clientIp = normalizeOptional(command.clientIp());
        validateStoreFields(null, storeName, address, contactPhone, clientIp);
        return new NormalizedUpdate(
                command.storeId(),
                storeName,
                address,
                contactPhone,
                command.operatorId(),
                clientIp
        );
    }

    private NormalizedStatusChange normalize(AdminStoreStatusChangeCommand command) {
        if (command == null || command.storeId() == null || command.storeId() <= 0
                || command.operatorId() == null || command.operatorId() <= 0
                || (command.status() != StoreStatus.ACTIVE
                && command.status() != StoreStatus.SUSPENDED)) {
            throw new CustomException(ExceptionEnum.PLATFORM_STORE_STATUS_INVALID);
        }
        String clientIp = normalizeOptional(command.clientIp());
        if (clientIp != null && clientIp.length() > 45) {
            throw new CustomException(ExceptionEnum.PLATFORM_INVALID_REQUEST);
        }
        return new NormalizedStatusChange(
                command.storeId(),
                command.status(),
                command.operatorId(),
                clientIp
        );
    }

    private void validateStoreFields(
            String storeCode,
            String storeName,
            String address,
            String contactPhone,
            String clientIp
    ) {
        if ((storeCode != null && (storeCode.length() > 64
                || !STORE_CODE_PATTERN.matcher(storeCode).matches()))
                || storeName == null || storeName.length() > 200
                || address == null || address.length() > 500
                || contactPhone == null || !PHONE_PATTERN.matcher(contactPhone).matches()
                || (clientIp != null && clientIp.length() > 45)) {
            throw new CustomException(ExceptionEnum.PLATFORM_INVALID_REQUEST);
        }
    }

    private void validatePage(int pageNum, int pageSize) {
        if (pageNum < 1 || pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw new CustomException(ExceptionEnum.PLATFORM_INVALID_REQUEST);
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

    private Merchant requireActiveMerchant(Long merchantId) {
        Merchant merchant = requireMerchant(merchantId);
        if (merchant.getStatus() != MerchantStatus.ACTIVE) {
            throw new CustomException(ExceptionEnum.PLATFORM_MERCHANT_UNAVAILABLE);
        }
        return merchant;
    }

    private Merchant requireMerchant(Long merchantId) {
        Merchant merchant = merchantMapper.selectById(merchantId);
        if (merchant == null) {
            throw new CustomException(ExceptionEnum.PLATFORM_MERCHANT_NOT_FOUND);
        }
        return merchant;
    }

    private Store requireStoreForUpdate(Long storeId) {
        Store store = storeMapper.selectByIdForUpdate(storeId);
        if (store == null) {
            throw new CustomException(ExceptionEnum.PLATFORM_STORE_NOT_FOUND);
        }
        return store;
    }

    private Map<Long, Merchant> loadMerchants(Collection<Store> stores) {
        List<Long> merchantIds = stores.stream()
                .map(Store::getMerchantId)
                .distinct()
                .toList();
        if (merchantIds.isEmpty()) {
            return Map.of();
        }
        return merchantMapper.selectByIds(merchantIds).stream()
                .collect(Collectors.toMap(Merchant::getId, Function.identity()));
    }

    private AdminStoreManagementView toView(Store store, Merchant merchant) {
        return new AdminStoreManagementView(
                store.getId(),
                store.getMerchantId(),
                merchant == null ? null : merchant.getMerchantCode(),
                merchant == null ? null : merchant.getBusinessName(),
                store.getStoreCode(),
                store.getStoreName(),
                store.getAddress(),
                store.getContactPhone(),
                store.getStatus(),
                store.getCreateTime(),
                store.getUpdateTime()
        );
    }

    private Map<String, Object> snapshot(Store store) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("storeId", store.getId());
        snapshot.put("merchantId", store.getMerchantId());
        snapshot.put("storeCode", store.getStoreCode());
        snapshot.put("storeName", store.getStoreName());
        snapshot.put("address", store.getAddress());
        snapshot.put("contactPhone", store.getContactPhone());
        snapshot.put("status", store.getStatus() == null ? null : store.getStatus().getCode());
        return snapshot;
    }

    private AuditLog buildAudit(
            Store store,
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
                .storeId(store.getId())
                .action(action)
                .resourceType("STORE")
                .resourceNo(store.getStoreCode())
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
            throw new IllegalStateException("门店审计快照序列化失败", ex);
        }
    }

    private String normalizeRequired(String value) {
        String normalized = normalizeOptional(value);
        if (normalized == null) {
            throw new CustomException(ExceptionEnum.PLATFORM_INVALID_REQUEST);
        }
        return normalized;
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
            throw new CustomException(ExceptionEnum.PLATFORM_STORE_MANAGEMENT_WRITE_FAILED);
        }
    }

    private record NormalizedCreate(
            Long merchantId,
            String storeCode,
            String storeName,
            String address,
            String contactPhone,
            Long operatorId,
            String clientIp
    ) {
    }

    private record NormalizedUpdate(
            Long storeId,
            String storeName,
            String address,
            String contactPhone,
            Long operatorId,
            String clientIp
    ) {
    }

    private record NormalizedStatusChange(
            Long storeId,
            StoreStatus status,
            Long operatorId,
            String clientIp
    ) {
    }
}
