package com.core.coreboot.platform.customer.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.core.coreboot.exception.CustomException;
import com.core.coreboot.exception.ExceptionEnum;
import com.core.coreboot.platform.audit.entity.AuditLog;
import com.core.coreboot.platform.audit.mapper.AuditLogMapper;
import com.core.coreboot.platform.common.enums.AuditActorType;
import com.core.coreboot.platform.common.enums.CustomerStatus;
import com.core.coreboot.platform.common.enums.RoleCode;
import com.core.coreboot.platform.common.enums.SysUserStatus;
import com.core.coreboot.platform.common.model.PageResult;
import com.core.coreboot.platform.consumption.mapper.ConsumptionOrderMapper;
import com.core.coreboot.platform.consumption.model.CustomerConsumptionOrderView;
import com.core.coreboot.platform.customer.entity.CustomerSecurity;
import com.core.coreboot.platform.customer.entity.CustomerUser;
import com.core.coreboot.platform.customer.mapper.CustomerSecurityMapper;
import com.core.coreboot.platform.customer.mapper.CustomerUserMapper;
import com.core.coreboot.platform.customer.model.AdminCustomerManagementView;
import com.core.coreboot.platform.customer.model.AdminCustomerStatusChangeCommand;
import com.core.coreboot.platform.customer.service.AdminCustomerManagementService;
import com.core.coreboot.platform.customer.service.CustomerTransactionQueryService;
import com.core.coreboot.platform.point.entity.PointAccount;
import com.core.coreboot.platform.point.mapper.PointAccountMapper;
import com.core.coreboot.platform.point.model.CustomerPointLedgerView;
import com.core.coreboot.platform.recharge.model.CustomerRechargeOrderView;
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
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class AdminCustomerManagementServiceImpl implements AdminCustomerManagementService {
    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_KEYWORD_LENGTH = 100;
    private static final int MAX_CLIENT_IP_LENGTH = 45;
    private static final int MAX_REASON_LENGTH = 500;

    private final CustomerUserMapper customerUserMapper;
    private final CustomerSecurityMapper customerSecurityMapper;
    private final PointAccountMapper pointAccountMapper;
    private final ConsumptionOrderMapper consumptionOrderMapper;
    private final CustomerTransactionQueryService transactionQueryService;
    private final SysUserMapper sysUserMapper;
    private final StaffAuthorityMapper staffAuthorityMapper;
    private final AuditLogMapper auditLogMapper;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Autowired
    public AdminCustomerManagementServiceImpl(
            CustomerUserMapper customerUserMapper,
            CustomerSecurityMapper customerSecurityMapper,
            PointAccountMapper pointAccountMapper,
            ConsumptionOrderMapper consumptionOrderMapper,
            CustomerTransactionQueryService transactionQueryService,
            SysUserMapper sysUserMapper,
            StaffAuthorityMapper staffAuthorityMapper,
            AuditLogMapper auditLogMapper,
            ObjectMapper objectMapper
    ) {
        this(
                customerUserMapper,
                customerSecurityMapper,
                pointAccountMapper,
                consumptionOrderMapper,
                transactionQueryService,
                sysUserMapper,
                staffAuthorityMapper,
                auditLogMapper,
                objectMapper,
                Clock.systemDefaultZone()
        );
    }

    AdminCustomerManagementServiceImpl(
            CustomerUserMapper customerUserMapper,
            CustomerSecurityMapper customerSecurityMapper,
            PointAccountMapper pointAccountMapper,
            ConsumptionOrderMapper consumptionOrderMapper,
            CustomerTransactionQueryService transactionQueryService,
            SysUserMapper sysUserMapper,
            StaffAuthorityMapper staffAuthorityMapper,
            AuditLogMapper auditLogMapper,
            ObjectMapper objectMapper,
            Clock clock
    ) {
        this.customerUserMapper = customerUserMapper;
        this.customerSecurityMapper = customerSecurityMapper;
        this.pointAccountMapper = pointAccountMapper;
        this.consumptionOrderMapper = consumptionOrderMapper;
        this.transactionQueryService = transactionQueryService;
        this.sysUserMapper = sysUserMapper;
        this.staffAuthorityMapper = staffAuthorityMapper;
        this.auditLogMapper = auditLogMapper;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Override
    public PageResult<AdminCustomerManagementView> list(
            Long operatorId,
            int pageNum,
            int pageSize,
            CustomerStatus status,
            String keyword
    ) {
        requireSuperAdmin(operatorId);
        validatePage(pageNum, pageSize);
        String normalizedKeyword = normalizeOptional(keyword);
        if (normalizedKeyword != null && normalizedKeyword.length() > MAX_KEYWORD_LENGTH) {
            throw new CustomException(ExceptionEnum.PLATFORM_INVALID_REQUEST);
        }

        LambdaQueryWrapper<CustomerUser> wrapper = Wrappers.lambdaQuery(CustomerUser.class)
                .eq(status != null, CustomerUser::getStatus, status)
                .and(normalizedKeyword != null, condition -> condition
                        .like(CustomerUser::getPhone, normalizedKeyword)
                        .or()
                        .like(CustomerUser::getNickname, normalizedKeyword))
                .orderByDesc(CustomerUser::getCreateTime)
                .orderByDesc(CustomerUser::getId);
        IPage<CustomerUser> page = customerUserMapper.selectPage(
                new Page<>(pageNum, pageSize),
                wrapper
        );
        Map<Long, PointAccount> accounts = loadAccounts(page.getRecords());
        Map<Long, CustomerSecurity> securitySettings = loadSecuritySettings(page.getRecords());
        List<AdminCustomerManagementView> items = page.getRecords().stream()
                .map(customer -> toView(
                        customer,
                        accounts.get(customer.getId()),
                        securitySettings.get(customer.getId())
                ))
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
    public AdminCustomerManagementView get(Long operatorId, Long customerId) {
        requireSuperAdmin(operatorId);
        CustomerUser customer = requireCustomer(customerId);
        return toView(
                customer,
                pointAccountMapper.selectByCustomerId(customerId),
                customerSecurityMapper.selectById(customerId)
        );
    }

    @Override
    public PageResult<CustomerPointLedgerView> getPointLedger(
            Long operatorId,
            Long customerId,
            int pageNum,
            int pageSize
    ) {
        requireTransactionAccess(operatorId, customerId);
        return transactionQueryService.getPointLedger(customerId, pageNum, pageSize);
    }

    @Override
    public PageResult<CustomerRechargeOrderView> getRechargeOrders(
            Long operatorId,
            Long customerId,
            int pageNum,
            int pageSize
    ) {
        requireTransactionAccess(operatorId, customerId);
        return transactionQueryService.getRechargeOrders(customerId, pageNum, pageSize);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PageResult<CustomerConsumptionOrderView> getConsumptionOrders(
            Long operatorId,
            Long customerId,
            int pageNum,
            int pageSize
    ) {
        requireTransactionAccess(operatorId, customerId);
        return transactionQueryService.getConsumptionOrders(customerId, pageNum, pageSize);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminCustomerManagementView changeStatus(AdminCustomerStatusChangeCommand command) {
        NormalizedStatusChange normalized = normalize(command);
        requireSuperAdmin(normalized.operatorId());
        CustomerUser customer = requireCustomerForUpdate(normalized.customerId());
        PointAccount account = pointAccountMapper.selectByCustomerId(normalized.customerId());
        CustomerSecurity security = customerSecurityMapper.selectById(normalized.customerId());
        if (customer.getStatus() == normalized.status()) {
            return toView(customer, account, security);
        }

        Map<String, Object> before = snapshot(customer);
        LocalDateTime now = LocalDateTime.now(clock);
        requireOneRow(customerUserMapper.updateCustomerStatus(
                customer.getId(),
                normalized.status().getCode(),
                now
        ));
        int cancelledPendingOrders = normalized.status() == CustomerStatus.DISABLED
                ? consumptionOrderMapper.cancelPendingByCustomerId(customer.getId(), now)
                : 0;
        customer.setStatus(normalized.status());
        customer.setTokenVersion(requireTokenVersion(customer) + 1);
        customer.setUpdateTime(now);
        Map<String, Object> after = snapshot(customer);
        after.put("cancelledPendingConsumptionOrders", cancelledPendingOrders);
        requireOneRow(auditLogMapper.insert(buildAudit(
                customer,
                normalized.operatorId(),
                normalized.clientIp(),
                normalized.status() == CustomerStatus.ACTIVE
                        ? "CUSTOMER_ENABLED"
                        : "CUSTOMER_DISABLED",
                before,
                after,
                normalized.reason()
        )));
        return toView(customer, account, security);
    }

    private void requireTransactionAccess(Long operatorId, Long customerId) {
        requireSuperAdmin(operatorId);
        requireCustomer(customerId);
    }

    private NormalizedStatusChange normalize(AdminCustomerStatusChangeCommand command) {
        if (command == null || command.customerId() == null || command.customerId() <= 0
                || command.operatorId() == null || command.operatorId() <= 0
                || (command.status() != CustomerStatus.ACTIVE
                && command.status() != CustomerStatus.DISABLED)) {
            throw new CustomException(ExceptionEnum.PLATFORM_CUSTOMER_STATUS_INVALID);
        }
        String reason = normalizeOptional(command.reason());
        String clientIp = normalizeOptional(command.clientIp());
        if (reason == null || reason.length() > MAX_REASON_LENGTH
                || (clientIp != null && clientIp.length() > MAX_CLIENT_IP_LENGTH)) {
            throw new CustomException(ExceptionEnum.PLATFORM_CUSTOMER_STATUS_INVALID);
        }
        return new NormalizedStatusChange(
                command.customerId(),
                command.status(),
                reason,
                command.operatorId(),
                clientIp
        );
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

    private CustomerUser requireCustomer(Long customerId) {
        if (customerId == null || customerId <= 0) {
            throw new CustomException(ExceptionEnum.PLATFORM_CUSTOMER_NOT_FOUND);
        }
        CustomerUser customer = customerUserMapper.selectById(customerId);
        if (customer == null) {
            throw new CustomException(ExceptionEnum.PLATFORM_CUSTOMER_NOT_FOUND);
        }
        return customer;
    }

    private CustomerUser requireCustomerForUpdate(Long customerId) {
        if (customerId == null || customerId <= 0) {
            throw new CustomException(ExceptionEnum.PLATFORM_CUSTOMER_NOT_FOUND);
        }
        CustomerUser customer = customerUserMapper.selectByIdForUpdate(customerId);
        if (customer == null) {
            throw new CustomException(ExceptionEnum.PLATFORM_CUSTOMER_NOT_FOUND);
        }
        return customer;
    }

    private Map<Long, PointAccount> loadAccounts(Collection<CustomerUser> customers) {
        List<Long> customerIds = customerIds(customers);
        if (customerIds.isEmpty()) {
            return Map.of();
        }
        return pointAccountMapper.selectList(Wrappers.lambdaQuery(PointAccount.class)
                        .in(PointAccount::getCustomerId, customerIds))
                .stream()
                .collect(Collectors.toMap(PointAccount::getCustomerId, Function.identity()));
    }

    private Map<Long, CustomerSecurity> loadSecuritySettings(Collection<CustomerUser> customers) {
        List<Long> customerIds = customerIds(customers);
        if (customerIds.isEmpty()) {
            return Map.of();
        }
        return customerSecurityMapper.selectList(Wrappers.lambdaQuery(CustomerSecurity.class)
                        .in(CustomerSecurity::getCustomerId, customerIds))
                .stream()
                .collect(Collectors.toMap(CustomerSecurity::getCustomerId, Function.identity()));
    }

    private List<Long> customerIds(Collection<CustomerUser> customers) {
        return customers.stream().map(CustomerUser::getId).toList();
    }

    private AdminCustomerManagementView toView(
            CustomerUser customer,
            PointAccount account,
            CustomerSecurity security
    ) {
        long availablePoints = account == null || account.getAvailablePoints() == null
                ? 0L
                : account.getAvailablePoints();
        boolean pinConfigured = security != null
                && security.getConsumePinHash() != null
                && !security.getConsumePinHash().isBlank();
        LocalDateTime lockedUntil = security == null ? null : security.getLockedUntil();
        return new AdminCustomerManagementView(
                customer.getId(),
                customer.getPhone(),
                customer.getNickname(),
                customer.getAvatarUrl(),
                customer.getStatus(),
                availablePoints,
                pinConfigured,
                lockedUntil != null && lockedUntil.isAfter(LocalDateTime.now(clock)),
                lockedUntil,
                customer.getLastLoginTime(),
                customer.getCreateTime(),
                customer.getUpdateTime()
        );
    }

    private int requireTokenVersion(CustomerUser customer) {
        return customer.getTokenVersion() == null ? 0 : customer.getTokenVersion();
    }

    private Map<String, Object> snapshot(CustomerUser customer) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("customerId", customer.getId());
        snapshot.put("status", customer.getStatus() == null ? null : customer.getStatus().getCode());
        snapshot.put("tokenVersion", requireTokenVersion(customer));
        return snapshot;
    }

    private AuditLog buildAudit(
            CustomerUser customer,
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
                .resourceType("CUSTOMER")
                .resourceNo(String.valueOf(customer.getId()))
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
            throw new IllegalStateException("客户审计快照序列化失败", ex);
        }
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
            throw new CustomException(ExceptionEnum.PLATFORM_CUSTOMER_MANAGEMENT_WRITE_FAILED);
        }
    }

    private record NormalizedStatusChange(
            Long customerId,
            CustomerStatus status,
            String reason,
            Long operatorId,
            String clientIp
    ) {
    }
}
