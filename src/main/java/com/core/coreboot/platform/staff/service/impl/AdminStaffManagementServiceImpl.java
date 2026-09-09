package com.core.coreboot.platform.staff.service.impl;

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
import com.core.coreboot.platform.staff.entity.SysRole;
import com.core.coreboot.platform.staff.entity.SysUser;
import com.core.coreboot.platform.staff.entity.SysUserRole;
import com.core.coreboot.platform.staff.entity.SysUserStore;
import com.core.coreboot.platform.staff.mapper.StaffAuthorityMapper;
import com.core.coreboot.platform.staff.mapper.SysRoleMapper;
import com.core.coreboot.platform.staff.mapper.SysUserMapper;
import com.core.coreboot.platform.staff.mapper.SysUserRoleMapper;
import com.core.coreboot.platform.staff.mapper.SysUserStoreMapper;
import com.core.coreboot.platform.staff.model.AdminStaffCreateCommand;
import com.core.coreboot.platform.staff.model.AdminStaffManagementView;
import com.core.coreboot.platform.staff.model.AdminStaffPasswordResetCommand;
import com.core.coreboot.platform.staff.model.AdminStaffStatusChangeCommand;
import com.core.coreboot.platform.staff.model.AdminStaffStoreView;
import com.core.coreboot.platform.staff.model.AdminStaffUnlockCommand;
import com.core.coreboot.platform.staff.model.AdminStaffUpdateCommand;
import com.core.coreboot.platform.staff.service.AdminStaffManagementService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class AdminStaffManagementServiceImpl implements AdminStaffManagementService {
    private static final int MAX_PAGE_SIZE = 100;
    private static final Pattern USERNAME_PATTERN = Pattern.compile("[A-Za-z0-9._-]{3,64}");
    private static final Pattern PHONE_PATTERN = Pattern.compile("[0-9+()\\-\\s]{5,32}");

    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final SysUserStoreMapper sysUserStoreMapper;
    private final StaffAuthorityMapper staffAuthorityMapper;
    private final StoreMapper storeMapper;
    private final MerchantMapper merchantMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogMapper auditLogMapper;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Autowired
    public AdminStaffManagementServiceImpl(
            SysUserMapper sysUserMapper,
            SysRoleMapper sysRoleMapper,
            SysUserRoleMapper sysUserRoleMapper,
            SysUserStoreMapper sysUserStoreMapper,
            StaffAuthorityMapper staffAuthorityMapper,
            StoreMapper storeMapper,
            MerchantMapper merchantMapper,
            PasswordEncoder passwordEncoder,
            AuditLogMapper auditLogMapper,
            ObjectMapper objectMapper
    ) {
        this(
                sysUserMapper,
                sysRoleMapper,
                sysUserRoleMapper,
                sysUserStoreMapper,
                staffAuthorityMapper,
                storeMapper,
                merchantMapper,
                passwordEncoder,
                auditLogMapper,
                objectMapper,
                Clock.systemDefaultZone()
        );
    }

    AdminStaffManagementServiceImpl(
            SysUserMapper sysUserMapper,
            SysRoleMapper sysRoleMapper,
            SysUserRoleMapper sysUserRoleMapper,
            SysUserStoreMapper sysUserStoreMapper,
            StaffAuthorityMapper staffAuthorityMapper,
            StoreMapper storeMapper,
            MerchantMapper merchantMapper,
            PasswordEncoder passwordEncoder,
            AuditLogMapper auditLogMapper,
            ObjectMapper objectMapper,
            Clock clock
    ) {
        this.sysUserMapper = sysUserMapper;
        this.sysRoleMapper = sysRoleMapper;
        this.sysUserRoleMapper = sysUserRoleMapper;
        this.sysUserStoreMapper = sysUserStoreMapper;
        this.staffAuthorityMapper = staffAuthorityMapper;
        this.storeMapper = storeMapper;
        this.merchantMapper = merchantMapper;
        this.passwordEncoder = passwordEncoder;
        this.auditLogMapper = auditLogMapper;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Override
    public PageResult<AdminStaffManagementView> list(
            Long operatorId,
            int pageNum,
            int pageSize,
            RoleCode roleCode,
            SysUserStatus status,
            Long storeId,
            String keyword
    ) {
        requireSuperAdmin(operatorId);
        validatePage(pageNum, pageSize);
        if (storeId != null && storeId <= 0) {
            throw new CustomException(ExceptionEnum.PLATFORM_INVALID_REQUEST);
        }
        String normalizedKeyword = normalizeOptional(keyword);
        if (normalizedKeyword != null && normalizedKeyword.length() > 100) {
            throw new CustomException(ExceptionEnum.PLATFORM_INVALID_REQUEST);
        }

        LambdaQueryWrapper<SysUser> wrapper = Wrappers.lambdaQuery(SysUser.class)
                .eq(status != null, SysUser::getStatus, status)
                .and(normalizedKeyword != null, condition -> condition
                        .like(SysUser::getUsername, normalizedKeyword)
                        .or()
                        .like(SysUser::getRealName, normalizedKeyword)
                        .or()
                        .like(SysUser::getPhone, normalizedKeyword));
        if (roleCode != null) {
            wrapper.inSql(SysUser::getId, roleSubquery(roleCode));
        }
        if (storeId != null) {
            wrapper.inSql(SysUser::getId, storeSubquery(storeId));
        }
        wrapper.orderByDesc(SysUser::getCreateTime).orderByDesc(SysUser::getId);
        IPage<SysUser> page = sysUserMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        List<AdminStaffManagementView> items = buildViews(page.getRecords());
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
    public List<AdminStaffStoreView> listAssignableStores(Long operatorId) {
        requireSuperAdmin(operatorId);
        List<Store> stores = storeMapper.selectList(Wrappers.lambdaQuery(Store.class)
                .ne(Store::getStatus, StoreStatus.CLOSED)
                .orderByAsc(Store::getStoreName)
                .orderByAsc(Store::getId));
        Map<Long, Merchant> merchants = loadMerchants(stores);
        return stores.stream()
                .map(store -> toStoreView(store, merchants.get(store.getMerchantId())))
                .filter(view -> view.merchantStatus() != null
                        && view.merchantStatus() != MerchantStatus.TERMINATED)
                .sorted(Comparator
                        .comparing(AdminStaffStoreView::merchantName, Comparator.nullsLast(String::compareTo))
                        .thenComparing(AdminStaffStoreView::storeName)
                        .thenComparing(AdminStaffStoreView::storeId))
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminStaffManagementView create(AdminStaffCreateCommand command) {
        NormalizedCreate normalized = normalize(command);
        requireSuperAdmin(normalized.operatorId());
        SysRole role = requireActiveStaffRole(normalized.roleCode());
        StoreScope storeScope = requireAssignableStore(normalized.storeId());
        requireUsernameAvailable(normalized.username());
        requirePhoneAvailable(normalized.phone(), null);

        LocalDateTime now = LocalDateTime.now(clock);
        SysUser user = SysUser.builder()
                .username(normalized.username())
                .phone(normalized.phone())
                .passwordHash(passwordEncoder.encode(normalized.initialPassword()))
                .realName(normalized.realName())
                .status(SysUserStatus.ACTIVE)
                .failedLoginCount(0)
                .tokenVersion(0)
                .passwordUpdatedTime(now)
                .createTime(now)
                .updateTime(now)
                .build();
        try {
            requireOneRow(sysUserMapper.insert(user));
            requireOneRow(sysUserRoleMapper.insert(SysUserRole.builder()
                    .userId(user.getId())
                    .roleId(role.getId())
                    .createTime(now)
                    .build()));
            requireOneRow(sysUserStoreMapper.insert(SysUserStore.builder()
                    .userId(user.getId())
                    .storeId(storeScope.store().getId())
                    .createTime(now)
                    .build()));
        } catch (DuplicateKeyException ex) {
            throw new CustomException(ExceptionEnum.PLATFORM_DATA_CONFLICT);
        }

        List<RoleCode> roles = List.of(normalized.roleCode());
        List<AdminStaffStoreView> stores = List.of(toStoreView(storeScope.store(), storeScope.merchant()));
        requireOneRow(auditLogMapper.insert(buildAudit(
                user,
                normalized.operatorId(),
                normalized.clientIp(),
                "STAFF_CREATED",
                null,
                snapshot(user, roles, stores),
                "创建员工账号",
                storeScope.store().getId()
        )));
        return toView(user, roles, stores);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminStaffManagementView update(AdminStaffUpdateCommand command) {
        NormalizedUpdate normalized = normalize(command);
        requireSuperAdmin(normalized.operatorId());
        SysUser user = requireUserForUpdate(normalized.userId());
        StaffAssignments assignments = requireManagedStaff(user.getId());
        SysRole targetRole = requireActiveStaffRole(normalized.roleCode());
        StoreScope targetStore = requireAssignableStore(normalized.storeId());
        requirePhoneAvailable(normalized.phone(), user.getId());

        List<AdminStaffStoreView> beforeStores = toStoreViews(assignments.storeIds());
        Map<String, Object> before = snapshot(user, assignments.roleCodes(), beforeStores);
        boolean roleChanged = assignments.roleCodes().size() != 1
                || assignments.roleCodes().getFirst() != normalized.roleCode();
        boolean storeChanged = assignments.storeIds().size() != 1
                || !Objects.equals(assignments.storeIds().getFirst(), normalized.storeId());
        int tokenVersionIncrement = roleChanged || storeChanged ? 1 : 0;
        LocalDateTime now = LocalDateTime.now(clock);

        requireOneRow(sysUserMapper.updateStaffProfile(
                user.getId(),
                normalized.phone(),
                normalized.realName(),
                tokenVersionIncrement,
                now
        ));
        if (roleChanged) {
            sysUserRoleMapper.delete(Wrappers.lambdaQuery(SysUserRole.class)
                    .eq(SysUserRole::getUserId, user.getId()));
            requireOneRow(sysUserRoleMapper.insert(SysUserRole.builder()
                    .userId(user.getId())
                    .roleId(targetRole.getId())
                    .createTime(now)
                    .build()));
        }
        if (storeChanged) {
            sysUserStoreMapper.delete(Wrappers.lambdaQuery(SysUserStore.class)
                    .eq(SysUserStore::getUserId, user.getId()));
            requireOneRow(sysUserStoreMapper.insert(SysUserStore.builder()
                    .userId(user.getId())
                    .storeId(targetStore.store().getId())
                    .createTime(now)
                    .build()));
        }

        user.setPhone(normalized.phone());
        user.setRealName(normalized.realName());
        user.setTokenVersion(safeTokenVersion(user) + tokenVersionIncrement);
        user.setUpdateTime(now);
        List<RoleCode> roles = List.of(normalized.roleCode());
        List<AdminStaffStoreView> stores = List.of(toStoreView(targetStore.store(), targetStore.merchant()));
        requireOneRow(auditLogMapper.insert(buildAudit(
                user,
                normalized.operatorId(),
                normalized.clientIp(),
                "STAFF_UPDATED",
                before,
                snapshot(user, roles, stores),
                "修改员工资料及门店权限",
                targetStore.store().getId()
        )));
        return toView(user, roles, stores);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminStaffManagementView changeStatus(AdminStaffStatusChangeCommand command) {
        NormalizedStatusChange normalized = normalize(command);
        requireSuperAdmin(normalized.operatorId());
        SysUser user = requireUserForUpdate(normalized.userId());
        StaffAssignments assignments = requireManagedStaff(user.getId());
        List<AdminStaffStoreView> stores = toStoreViews(assignments.storeIds());
        if (user.getStatus() == normalized.status() && !isLoginLocked(user)) {
            return toView(user, assignments.roleCodes(), stores);
        }

        Map<String, Object> before = snapshot(user, assignments.roleCodes(), stores);
        LocalDateTime now = LocalDateTime.now(clock);
        requireOneRow(sysUserMapper.updateStaffStatus(user.getId(), normalized.status().getCode(), now));
        user.setStatus(normalized.status());
        user.setFailedLoginCount(0);
        user.setLockedUntil(null);
        user.setTokenVersion(safeTokenVersion(user) + 1);
        user.setUpdateTime(now);
        boolean enabled = normalized.status() == SysUserStatus.ACTIVE;
        requireOneRow(auditLogMapper.insert(buildAudit(
                user,
                normalized.operatorId(),
                normalized.clientIp(),
                enabled ? "STAFF_ENABLED" : "STAFF_DISABLED",
                before,
                snapshot(user, assignments.roleCodes(), stores),
                enabled ? "启用员工账号" : "停用员工账号",
                firstStoreId(assignments.storeIds())
        )));
        return toView(user, assignments.roleCodes(), stores);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(AdminStaffPasswordResetCommand command) {
        NormalizedPasswordReset normalized = normalize(command);
        requireSuperAdmin(normalized.operatorId());
        SysUser user = requireUserForUpdate(normalized.userId());
        StaffAssignments assignments = requireManagedStaff(user.getId());
        if (passwordEncoder.matches(normalized.newPassword(), user.getPasswordHash())) {
            throw new CustomException(ExceptionEnum.PLATFORM_STAFF_PASSWORD_UNCHANGED);
        }
        List<AdminStaffStoreView> stores = toStoreViews(assignments.storeIds());
        Map<String, Object> before = snapshot(user, assignments.roleCodes(), stores);
        LocalDateTime now = LocalDateTime.now(clock);
        requireOneRow(sysUserMapper.resetStaffPassword(
                user.getId(), passwordEncoder.encode(normalized.newPassword()), now
        ));
        user.setFailedLoginCount(0);
        user.setLockedUntil(null);
        user.setTokenVersion(safeTokenVersion(user) + 1);
        user.setPasswordUpdatedTime(now);
        user.setUpdateTime(now);
        requireOneRow(auditLogMapper.insert(buildAudit(
                user,
                normalized.operatorId(),
                normalized.clientIp(),
                "STAFF_PASSWORD_RESET",
                before,
                snapshot(user, assignments.roleCodes(), stores),
                "重置员工登录密码，未记录密码内容",
                firstStoreId(assignments.storeIds())
        )));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminStaffManagementView unlock(AdminStaffUnlockCommand command) {
        NormalizedUnlock normalized = normalize(command);
        requireSuperAdmin(normalized.operatorId());
        SysUser user = requireUserForUpdate(normalized.userId());
        StaffAssignments assignments = requireManagedStaff(user.getId());
        List<AdminStaffStoreView> stores = toStoreViews(assignments.storeIds());
        if (!isLoginLocked(user) && (user.getFailedLoginCount() == null || user.getFailedLoginCount() == 0)) {
            return toView(user, assignments.roleCodes(), stores);
        }

        Map<String, Object> before = snapshot(user, assignments.roleCodes(), stores);
        LocalDateTime now = LocalDateTime.now(clock);
        requireOneRow(sysUserMapper.unlockStaffLogin(user.getId(), now));
        if (user.getStatus() == SysUserStatus.LOCKED) {
            user.setStatus(SysUserStatus.ACTIVE);
        }
        user.setFailedLoginCount(0);
        user.setLockedUntil(null);
        user.setTokenVersion(safeTokenVersion(user) + 1);
        user.setUpdateTime(now);
        requireOneRow(auditLogMapper.insert(buildAudit(
                user,
                normalized.operatorId(),
                normalized.clientIp(),
                "STAFF_LOGIN_UNLOCKED",
                before,
                snapshot(user, assignments.roleCodes(), stores),
                "解除员工登录锁定",
                firstStoreId(assignments.storeIds())
        )));
        return toView(user, assignments.roleCodes(), stores);
    }

    private NormalizedCreate normalize(AdminStaffCreateCommand command) {
        if (command == null || command.operatorId() == null || command.operatorId() <= 0
                || command.storeId() == null || command.storeId() <= 0) {
            throw new CustomException(ExceptionEnum.PLATFORM_INVALID_REQUEST);
        }
        String username = normalizeRequired(command.username()).toLowerCase(Locale.ROOT);
        String realName = normalizeRequired(command.realName());
        String phone = normalizeOptional(command.phone());
        String clientIp = normalizeOptional(command.clientIp());
        validateStaffFields(username, realName, phone, command.roleCode(), clientIp);
        validateStrongPassword(command.initialPassword());
        return new NormalizedCreate(
                username,
                command.initialPassword(),
                realName,
                phone,
                command.roleCode(),
                command.storeId(),
                command.operatorId(),
                clientIp
        );
    }

    private NormalizedUpdate normalize(AdminStaffUpdateCommand command) {
        if (command == null || command.userId() == null || command.userId() <= 0
                || command.operatorId() == null || command.operatorId() <= 0
                || command.storeId() == null || command.storeId() <= 0) {
            throw new CustomException(ExceptionEnum.PLATFORM_INVALID_REQUEST);
        }
        String realName = normalizeRequired(command.realName());
        String phone = normalizeOptional(command.phone());
        String clientIp = normalizeOptional(command.clientIp());
        validateStaffFields(null, realName, phone, command.roleCode(), clientIp);
        return new NormalizedUpdate(
                command.userId(), realName, phone, command.roleCode(), command.storeId(),
                command.operatorId(), clientIp
        );
    }

    private NormalizedStatusChange normalize(AdminStaffStatusChangeCommand command) {
        if (command == null || command.userId() == null || command.userId() <= 0
                || command.operatorId() == null || command.operatorId() <= 0
                || (command.status() != SysUserStatus.ACTIVE
                && command.status() != SysUserStatus.DISABLED)) {
            throw new CustomException(ExceptionEnum.PLATFORM_STAFF_STATUS_INVALID);
        }
        String clientIp = normalizeClientIp(command.clientIp());
        return new NormalizedStatusChange(
                command.userId(), command.status(), command.operatorId(), clientIp
        );
    }

    private NormalizedPasswordReset normalize(AdminStaffPasswordResetCommand command) {
        if (command == null || command.userId() == null || command.userId() <= 0
                || command.operatorId() == null || command.operatorId() <= 0) {
            throw new CustomException(ExceptionEnum.PLATFORM_INVALID_REQUEST);
        }
        validateStrongPassword(command.newPassword());
        return new NormalizedPasswordReset(
                command.userId(), command.newPassword(), command.operatorId(), normalizeClientIp(command.clientIp())
        );
    }

    private NormalizedUnlock normalize(AdminStaffUnlockCommand command) {
        if (command == null || command.userId() == null || command.userId() <= 0
                || command.operatorId() == null || command.operatorId() <= 0) {
            throw new CustomException(ExceptionEnum.PLATFORM_INVALID_REQUEST);
        }
        return new NormalizedUnlock(
                command.userId(), command.operatorId(), normalizeClientIp(command.clientIp())
        );
    }

    private void validateStaffFields(
            String username,
            String realName,
            String phone,
            RoleCode roleCode,
            String clientIp
    ) {
        if (!isStaffRole(roleCode)) {
            throw new CustomException(ExceptionEnum.PLATFORM_STAFF_ROLE_INVALID);
        }
        if ((username != null && !USERNAME_PATTERN.matcher(username).matches())
                || realName.length() > 64
                || (phone != null && !PHONE_PATTERN.matcher(phone).matches())
                || (clientIp != null && clientIp.length() > 45)) {
            throw new CustomException(ExceptionEnum.PLATFORM_INVALID_REQUEST);
        }
    }

    private void validateStrongPassword(String password) {
        if (password == null || password.length() < 12 || password.length() > 128) {
            throw new CustomException(ExceptionEnum.PLATFORM_STAFF_PASSWORD_INVALID);
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
        if (!upper || !lower || !digit || !special) {
            throw new CustomException(ExceptionEnum.PLATFORM_STAFF_PASSWORD_INVALID);
        }
    }

    private boolean isStaffRole(RoleCode roleCode) {
        return roleCode == RoleCode.CLERK || roleCode == RoleCode.STORE_MANAGER;
    }

    private String roleSubquery(RoleCode roleCode) {
        return "SELECT ur.user_id FROM t_sys_user_role ur "
                + "INNER JOIN t_sys_role r ON r.id = ur.role_id "
                + "WHERE r.role_code = '" + roleCode.getCode() + "'";
    }

    private String storeSubquery(Long storeId) {
        return "SELECT us.user_id FROM t_sys_user_store us WHERE us.store_id = " + storeId;
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

    private SysRole requireActiveStaffRole(RoleCode roleCode) {
        if (!isStaffRole(roleCode)) {
            throw new CustomException(ExceptionEnum.PLATFORM_STAFF_ROLE_INVALID);
        }
        SysRole role = sysRoleMapper.selectByRoleCode(roleCode.getCode());
        if (role == null || !"ACTIVE".equals(role.getStatus())) {
            throw new CustomException(ExceptionEnum.PLATFORM_STAFF_ROLE_INVALID);
        }
        return role;
    }

    private StoreScope requireAssignableStore(Long storeId) {
        Store store = storeMapper.selectById(storeId);
        if (store == null || store.getStatus() == StoreStatus.CLOSED) {
            throw new CustomException(ExceptionEnum.PLATFORM_STAFF_STORE_SCOPE_INVALID);
        }
        Merchant merchant = merchantMapper.selectById(store.getMerchantId());
        if (merchant == null || merchant.getStatus() == MerchantStatus.TERMINATED) {
            throw new CustomException(ExceptionEnum.PLATFORM_STAFF_STORE_SCOPE_INVALID);
        }
        return new StoreScope(store, merchant);
    }

    private void requireUsernameAvailable(String username) {
        if (sysUserMapper.selectCount(Wrappers.lambdaQuery(SysUser.class)
                .eq(SysUser::getUsername, username)) > 0) {
            throw new CustomException(ExceptionEnum.PLATFORM_STAFF_USERNAME_USED);
        }
    }

    private void requirePhoneAvailable(String phone, Long excludedUserId) {
        if (phone == null) {
            return;
        }
        if (sysUserMapper.selectCount(Wrappers.lambdaQuery(SysUser.class)
                .eq(SysUser::getPhone, phone)
                .ne(excludedUserId != null, SysUser::getId, excludedUserId)) > 0) {
            throw new CustomException(ExceptionEnum.PLATFORM_STAFF_PHONE_USED);
        }
    }

    private SysUser requireUserForUpdate(Long userId) {
        SysUser user = sysUserMapper.selectByIdForUpdate(userId);
        if (user == null) {
            throw new CustomException(ExceptionEnum.PLATFORM_OPERATOR_NOT_FOUND);
        }
        return user;
    }

    private StaffAssignments requireManagedStaff(Long userId) {
        List<RoleCode> roles = loadRoleCodes(List.of(userId)).getOrDefault(userId, List.of());
        if (roles.contains(RoleCode.SUPER_ADMIN)) {
            throw new CustomException(ExceptionEnum.PLATFORM_STAFF_PROTECTED_ACCOUNT);
        }
        if (roles.isEmpty()) {
            throw new CustomException(ExceptionEnum.PLATFORM_STAFF_ROLE_INVALID);
        }
        List<Long> storeIds = sysUserStoreMapper.selectList(Wrappers.lambdaQuery(SysUserStore.class)
                        .eq(SysUserStore::getUserId, userId)
                        .orderByAsc(SysUserStore::getStoreId))
                .stream()
                .map(SysUserStore::getStoreId)
                .toList();
        return new StaffAssignments(roles, storeIds);
    }

    private List<AdminStaffManagementView> buildViews(List<SysUser> users) {
        if (users.isEmpty()) {
            return List.of();
        }
        List<Long> userIds = users.stream().map(SysUser::getId).toList();
        Map<Long, List<RoleCode>> roles = loadRoleCodes(userIds);
        Map<Long, List<Long>> storeIds = loadStoreIds(userIds);
        Set<Long> allStoreIds = storeIds.values().stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
        Map<Long, AdminStaffStoreView> stores = loadStoreViewMap(allStoreIds);
        return users.stream()
                .map(user -> toView(
                        user,
                        roles.getOrDefault(user.getId(), List.of()),
                        storeIds.getOrDefault(user.getId(), List.of()).stream()
                                .map(stores::get)
                                .filter(Objects::nonNull)
                                .toList()
                ))
                .toList();
    }

    private Map<Long, List<RoleCode>> loadRoleCodes(Collection<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        List<SysUserRole> links = sysUserRoleMapper.selectList(Wrappers.lambdaQuery(SysUserRole.class)
                .in(SysUserRole::getUserId, userIds));
        Set<Long> roleIds = links.stream().map(SysUserRole::getRoleId).collect(Collectors.toSet());
        if (roleIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, RoleCode> rolesById = sysRoleMapper.selectByIds(roleIds).stream()
                .collect(Collectors.toMap(SysRole::getId, SysRole::getRoleCode));
        Map<Long, List<RoleCode>> result = new HashMap<>();
        for (SysUserRole link : links) {
            RoleCode roleCode = rolesById.get(link.getRoleId());
            if (roleCode != null) {
                result.computeIfAbsent(link.getUserId(), ignored -> new ArrayList<>()).add(roleCode);
            }
        }
        result.values().forEach(values -> values.sort(Comparator.comparing(RoleCode::getCode)));
        return result;
    }

    private Map<Long, List<Long>> loadStoreIds(Collection<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        List<SysUserStore> links = sysUserStoreMapper.selectList(Wrappers.lambdaQuery(SysUserStore.class)
                .in(SysUserStore::getUserId, userIds)
                .orderByAsc(SysUserStore::getStoreId));
        Map<Long, List<Long>> result = new HashMap<>();
        for (SysUserStore link : links) {
            result.computeIfAbsent(link.getUserId(), ignored -> new ArrayList<>()).add(link.getStoreId());
        }
        return result;
    }

    private List<AdminStaffStoreView> toStoreViews(Collection<Long> storeIds) {
        if (storeIds.isEmpty()) {
            return List.of();
        }
        Map<Long, AdminStaffStoreView> viewMap = loadStoreViewMap(storeIds);
        return storeIds.stream().map(viewMap::get).filter(Objects::nonNull).toList();
    }

    private Map<Long, AdminStaffStoreView> loadStoreViewMap(Collection<Long> storeIds) {
        if (storeIds.isEmpty()) {
            return Map.of();
        }
        List<Store> stores = storeMapper.selectByIds(storeIds);
        Map<Long, Merchant> merchants = loadMerchants(stores);
        return stores.stream().collect(Collectors.toMap(
                Store::getId,
                store -> toStoreView(store, merchants.get(store.getMerchantId()))
        ));
    }

    private Map<Long, Merchant> loadMerchants(Collection<Store> stores) {
        List<Long> merchantIds = stores.stream()
                .map(Store::getMerchantId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (merchantIds.isEmpty()) {
            return Map.of();
        }
        return merchantMapper.selectByIds(merchantIds).stream()
                .collect(Collectors.toMap(Merchant::getId, Function.identity()));
    }

    private AdminStaffStoreView toStoreView(Store store, Merchant merchant) {
        return new AdminStaffStoreView(
                store.getId(),
                store.getStoreCode(),
                store.getStoreName(),
                store.getStatus(),
                store.getMerchantId(),
                merchant == null ? null : merchant.getBusinessName(),
                merchant == null ? null : merchant.getStatus()
        );
    }

    private AdminStaffManagementView toView(
            SysUser user,
            List<RoleCode> roleCodes,
            List<AdminStaffStoreView> stores
    ) {
        return new AdminStaffManagementView(
                user.getId(),
                user.getUsername(),
                user.getPhone(),
                user.getRealName(),
                user.getStatus(),
                roleCodes,
                stores,
                user.getFailedLoginCount() == null ? 0 : user.getFailedLoginCount(),
                user.getLockedUntil(),
                isLoginLocked(user),
                user.getLastLoginTime(),
                user.getPasswordUpdatedTime(),
                user.getCreateTime(),
                user.getUpdateTime()
        );
    }

    private boolean isLoginLocked(SysUser user) {
        return user.getStatus() == SysUserStatus.LOCKED
                || (user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now(clock)));
    }

    private int safeTokenVersion(SysUser user) {
        return user.getTokenVersion() == null ? 0 : user.getTokenVersion();
    }

    private Long firstStoreId(List<Long> storeIds) {
        return storeIds.isEmpty() ? null : storeIds.getFirst();
    }

    private Map<String, Object> snapshot(
            SysUser user,
            List<RoleCode> roleCodes,
            List<AdminStaffStoreView> stores
    ) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("userId", user.getId());
        snapshot.put("username", user.getUsername());
        snapshot.put("phone", user.getPhone());
        snapshot.put("realName", user.getRealName());
        snapshot.put("status", user.getStatus() == null ? null : user.getStatus().getCode());
        snapshot.put("roleCodes", roleCodes.stream().map(RoleCode::getCode).toList());
        snapshot.put("storeIds", stores.stream().map(AdminStaffStoreView::storeId).toList());
        snapshot.put("failedLoginCount", user.getFailedLoginCount());
        snapshot.put("lockedUntil", user.getLockedUntil());
        snapshot.put("tokenVersion", user.getTokenVersion());
        snapshot.put("passwordUpdatedTime", user.getPasswordUpdatedTime());
        return snapshot;
    }

    private AuditLog buildAudit(
            SysUser user,
            Long operatorId,
            String clientIp,
            String action,
            Map<String, Object> before,
            Map<String, Object> after,
            String remark,
            Long storeId
    ) {
        return AuditLog.builder()
                .actorType(AuditActorType.SYS_USER)
                .actorId(operatorId)
                .operatorRole(RoleCode.SUPER_ADMIN)
                .storeId(storeId)
                .action(action)
                .resourceType("SYS_USER")
                .resourceNo(user.getUsername())
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
            throw new IllegalStateException("员工账号审计快照序列化失败", ex);
        }
    }

    private String normalizeRequired(String value) {
        String normalized = normalizeOptional(value);
        if (normalized == null) {
            throw new CustomException(ExceptionEnum.PLATFORM_INVALID_REQUEST);
        }
        return normalized;
    }

    private String normalizeClientIp(String value) {
        String normalized = normalizeOptional(value);
        if (normalized != null && normalized.length() > 45) {
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
            throw new CustomException(ExceptionEnum.PLATFORM_STAFF_MANAGEMENT_WRITE_FAILED);
        }
    }

    private record NormalizedCreate(
            String username,
            String initialPassword,
            String realName,
            String phone,
            RoleCode roleCode,
            Long storeId,
            Long operatorId,
            String clientIp
    ) {
    }

    private record NormalizedUpdate(
            Long userId,
            String realName,
            String phone,
            RoleCode roleCode,
            Long storeId,
            Long operatorId,
            String clientIp
    ) {
    }

    private record NormalizedStatusChange(
            Long userId,
            SysUserStatus status,
            Long operatorId,
            String clientIp
    ) {
    }

    private record NormalizedPasswordReset(
            Long userId,
            String newPassword,
            Long operatorId,
            String clientIp
    ) {
    }

    private record NormalizedUnlock(Long userId, Long operatorId, String clientIp) {
    }

    private record StoreScope(Store store, Merchant merchant) {
    }

    private record StaffAssignments(List<RoleCode> roleCodes, List<Long> storeIds) {
    }
}
