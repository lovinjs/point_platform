package com.core.coreboot.platform.customer.auth.service;

import com.core.coreboot.exception.CustomException;
import com.core.coreboot.exception.ExceptionEnum;
import com.core.coreboot.platform.audit.entity.AuditLog;
import com.core.coreboot.platform.audit.mapper.AuditLogMapper;
import com.core.coreboot.platform.common.enums.AuditActorType;
import com.core.coreboot.platform.common.enums.CustomerStatus;
import com.core.coreboot.platform.customer.auth.model.WechatUserProfile;
import com.core.coreboot.platform.customer.entity.CustomerIdentity;
import com.core.coreboot.platform.customer.entity.CustomerUser;
import com.core.coreboot.platform.customer.mapper.CustomerIdentityMapper;
import com.core.coreboot.platform.customer.mapper.CustomerUserMapper;
import com.core.coreboot.platform.point.mapper.PointAccountMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class WechatCustomerIdentityServiceImpl implements WechatCustomerIdentityService {
    private static final String PROVIDER_CODE = "WECHAT_H5";

    private final CustomerIdentityMapper customerIdentityMapper;
    private final CustomerUserMapper customerUserMapper;
    private final PointAccountMapper pointAccountMapper;
    private final AuditLogMapper auditLogMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long resolveCustomer(String appId, WechatUserProfile profile) {
        String normalizedAppId = requireLength(appId, 64);
        String openId = requireLength(profile.openId(), 64);
        String unionId = optionalLength(profile.unionId(), 64);
        String nickname = optionalCodePointLength(profile.nickname(), 100);
        String avatarUrl = optionalLength(profile.avatarUrl(), 500);

        CustomerIdentity existingIdentity = customerIdentityMapper.selectByExternalIdentity(
                PROVIDER_CODE,
                normalizedAppId,
                openId
        );
        if (existingIdentity != null) {
            CustomerUser customer = requireActiveCustomer(existingIdentity.getCustomerId());
            updateIdentityUnionId(existingIdentity, unionId);
            updateProfile(customer, nickname, avatarUrl);
            return customer.getId();
        }

        CustomerUser customer = CustomerUser.builder()
                .nickname(nickname)
                .avatarUrl(avatarUrl)
                .status(CustomerStatus.ACTIVE)
                .tokenVersion(0)
                .build();
        requireOneRow(customerUserMapper.insert(customer));
        if (customer.getId() == null) {
            throw new IllegalStateException("创建客户后未返回主键");
        }
        requireOneRow(pointAccountMapper.ensureAccount(customer.getId()));

        CustomerIdentity identity = CustomerIdentity.builder()
                .customerId(customer.getId())
                .providerCode(PROVIDER_CODE)
                .appId(normalizedAppId)
                .externalId(openId)
                .unionId(unionId)
                .build();
        if (customerIdentityMapper.insertIgnore(identity) != 1) {
            throw new CustomException(ExceptionEnum.PLATFORM_DATA_CONFLICT);
        }
        insertCreationAudit(customer.getId());
        return customer.getId();
    }

    private CustomerUser requireActiveCustomer(Long customerId) {
        CustomerUser customer = customerUserMapper.selectById(customerId);
        if (customer == null) {
            throw new CustomException(ExceptionEnum.PLATFORM_CUSTOMER_NOT_FOUND);
        }
        if (customer.getStatus() != CustomerStatus.ACTIVE) {
            throw new CustomException(ExceptionEnum.PLATFORM_CUSTOMER_DISABLED);
        }
        return customer;
    }

    private void updateIdentityUnionId(CustomerIdentity identity, String unionId) {
        if (unionId == null || Objects.equals(unionId, identity.getUnionId())) {
            return;
        }
        identity.setUnionId(unionId);
        requireOneRow(customerIdentityMapper.updateById(identity));
    }

    private void updateProfile(CustomerUser customer, String nickname, String avatarUrl) {
        String nextNickname = nickname == null ? customer.getNickname() : nickname;
        String nextAvatarUrl = avatarUrl == null ? customer.getAvatarUrl() : avatarUrl;
        if (Objects.equals(nextNickname, customer.getNickname())
                && Objects.equals(nextAvatarUrl, customer.getAvatarUrl())) {
            return;
        }
        requireOneRow(customerUserMapper.updateWechatProfile(
                customer.getId(),
                nextNickname,
                nextAvatarUrl
        ));
    }

    private void insertCreationAudit(Long customerId) {
        AuditLog auditLog = AuditLog.builder()
                .actorType(AuditActorType.CUSTOMER)
                .actorId(customerId)
                .action("CUSTOMER_WECHAT_H5_REGISTERED")
                .resourceType("CUSTOMER")
                .resourceNo(String.valueOf(customerId))
                .remark("微信H5首次授权创建未绑定手机号客户")
                .build();
        requireOneRow(auditLogMapper.insert(auditLog));
    }

    private String requireLength(String value, int maxLength) {
        String normalized = optionalLength(value, maxLength);
        if (normalized == null) {
            throw new CustomException(ExceptionEnum.PLATFORM_WECHAT_AUTH_FAILED);
        }
        return normalized;
    }

    private String optionalLength(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw new CustomException(ExceptionEnum.PLATFORM_WECHAT_AUTH_FAILED);
        }
        return normalized;
    }

    private String optionalCodePointLength(String value, int maxCodePoints) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.codePointCount(0, normalized.length()) > maxCodePoints) {
            int end = normalized.offsetByCodePoints(0, maxCodePoints);
            return normalized.substring(0, end);
        }
        return normalized;
    }

    private void requireOneRow(int affectedRows) {
        if (affectedRows != 1) {
            throw new IllegalStateException("微信客户身份数据写入失败");
        }
    }
}
