package com.core.coreboot.platform.customer.phone.service;

import com.core.coreboot.exception.CustomException;
import com.core.coreboot.exception.ExceptionEnum;
import com.core.coreboot.platform.audit.entity.AuditLog;
import com.core.coreboot.platform.audit.mapper.AuditLogMapper;
import com.core.coreboot.platform.common.enums.AuditActorType;
import com.core.coreboot.platform.common.enums.CustomerStatus;
import com.core.coreboot.platform.customer.auth.model.CustomerAccountView;
import com.core.coreboot.platform.customer.auth.service.CustomerProfileService;
import com.core.coreboot.platform.customer.entity.CustomerUser;
import com.core.coreboot.platform.customer.mapper.CustomerUserMapper;
import com.core.coreboot.platform.customer.phone.config.PhoneVerificationProperties;
import com.core.coreboot.platform.customer.phone.model.PhoneVerificationDispatchResult;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class CustomerPhoneServiceImpl implements CustomerPhoneService {
    private static final Logger log = LoggerFactory.getLogger(CustomerPhoneServiceImpl.class);
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");
    private static final Pattern CODE_PATTERN = Pattern.compile("^\\d{6}$");
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final CustomerUserMapper customerUserMapper;
    private final AuditLogMapper auditLogMapper;
    private final CustomerProfileService customerProfileService;
    private final PhoneVerificationCodeStore verificationCodeStore;
    private final PhoneVerificationSender verificationSender;
    private final PhoneVerificationProperties properties;

    @Override
    public PhoneVerificationDispatchResult requestVerificationCode(Long customerId, String phone) {
        String normalizedPhone = normalizePhone(phone);
        CustomerUser customer = requireActiveCustomer(customerId, false);
        if (normalizeNullable(customer.getPhone()) != null) {
            throw new CustomException(ExceptionEnum.PLATFORM_PHONE_ALREADY_BOUND);
        }

        return dispatchVerificationCode(customerId, normalizedPhone);
    }

    @Override
    public PhoneVerificationDispatchResult requestBoundPhoneVerificationCode(Long customerId) {
        CustomerUser customer = requireActiveCustomer(customerId, false);
        return dispatchVerificationCode(customerId, requireBoundPhone(customer));
    }

    @Override
    public void verifyBoundPhoneVerificationCode(Long customerId, String verificationCode) {
        String normalizedCode = normalizeCode(verificationCode);
        CustomerUser customer = requireActiveCustomer(customerId, false);
        String phone = requireBoundPhone(customer);
        validateProperties();
        verificationCodeStore.verifyAndConsume(
                customerId,
                phone,
                normalizedCode,
                properties.getMaxVerifyAttempts()
        );
    }

    private PhoneVerificationDispatchResult dispatchVerificationCode(Long customerId, String phone) {

        validateProperties();
        verificationSender.validateReady();
        verificationCodeStore.reserveSend(customerId, phone, properties);

        String verificationCode = newVerificationCode();
        verificationCodeStore.saveCode(
                customerId,
                phone,
                verificationCode,
                properties.getCodeTtl()
        );
        try {
            verificationSender.send(phone, verificationCode, properties.getCodeTtl());
        } catch (CustomException ex) {
            invalidateAfterSendFailure(customerId, phone);
            throw ex;
        } catch (RuntimeException ex) {
            invalidateAfterSendFailure(customerId, phone);
            log.error("手机号验证码发送器执行失败", ex);
            throw new CustomException(ExceptionEnum.PLATFORM_SMS_SEND_FAILED);
        }

        return new PhoneVerificationDispatchResult(
                properties.getCodeTtl().toSeconds(),
                properties.getResendInterval().toSeconds()
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CustomerAccountView bindPhone(
            Long customerId,
            String phone,
            String verificationCode,
            String clientIp
    ) {
        String normalizedPhone = normalizePhone(phone);
        String normalizedCode = normalizeCode(verificationCode);
        validateProperties();

        CustomerUser customer = requireActiveCustomer(customerId, true);
        String currentPhone = normalizeNullable(customer.getPhone());
        if (normalizedPhone.equals(currentPhone)) {
            return customerProfileService.getProfile(customerId);
        }
        if (currentPhone != null) {
            throw new CustomException(ExceptionEnum.PLATFORM_PHONE_ALREADY_BOUND);
        }

        verificationCodeStore.verifyAndConsume(
                customerId,
                normalizedPhone,
                normalizedCode,
                properties.getMaxVerifyAttempts()
        );

        CustomerUser existingOwner = customerUserMapper.selectByPhoneForUpdate(normalizedPhone);
        if (existingOwner != null && !customerId.equals(existingOwner.getId())) {
            throw new CustomException(ExceptionEnum.PLATFORM_PHONE_ALREADY_USED);
        }

        try {
            if (customerUserMapper.bindPhoneIfUnbound(customerId, normalizedPhone) != 1) {
                throw new CustomException(ExceptionEnum.PLATFORM_PHONE_BIND_FAILED);
            }
        } catch (DuplicateKeyException ex) {
            throw new CustomException(ExceptionEnum.PLATFORM_PHONE_ALREADY_USED);
        }

        insertBindingAudit(customerId, normalizedPhone, clientIp);
        return customerProfileService.getProfile(customerId);
    }

    private CustomerUser requireActiveCustomer(Long customerId, boolean forUpdate) {
        if (customerId == null || customerId <= 0) {
            throw new CustomException(ExceptionEnum.PLATFORM_CUSTOMER_NOT_FOUND);
        }
        CustomerUser customer = forUpdate
                ? customerUserMapper.selectByIdForUpdate(customerId)
                : customerUserMapper.selectById(customerId);
        if (customer == null) {
            throw new CustomException(ExceptionEnum.PLATFORM_CUSTOMER_NOT_FOUND);
        }
        if (customer.getStatus() != CustomerStatus.ACTIVE) {
            throw new CustomException(ExceptionEnum.PLATFORM_CUSTOMER_DISABLED);
        }
        return customer;
    }

    private void validateProperties() {
        Duration codeTtl = properties.getCodeTtl();
        Duration resendInterval = properties.getResendInterval();
        boolean durationInvalid = codeTtl == null
                || codeTtl.compareTo(Duration.ofMinutes(1)) < 0
                || codeTtl.compareTo(Duration.ofMinutes(10)) > 0
                || resendInterval == null
                || resendInterval.compareTo(Duration.ofSeconds(30)) < 0
                || resendInterval.compareTo(Duration.ofMinutes(10)) > 0;
        boolean limitsInvalid = properties.getMaxVerifyAttempts() < 3
                || properties.getMaxVerifyAttempts() > 10
                || properties.getCustomerHourlyLimit() < 1
                || properties.getCustomerDailyLimit() < properties.getCustomerHourlyLimit()
                || properties.getPhoneHourlyLimit() < 1
                || properties.getPhoneDailyLimit() < properties.getPhoneHourlyLimit()
                || properties.getGlobalDailyLimit() < properties.getPhoneDailyLimit();
        if (durationInvalid || limitsInvalid) {
            throw new CustomException(ExceptionEnum.PLATFORM_PHONE_VERIFICATION_NOT_CONFIGURED);
        }
    }

    private String normalizePhone(String phone) {
        String normalized = phone == null ? "" : phone.trim();
        if (!PHONE_PATTERN.matcher(normalized).matches()) {
            throw new CustomException(ExceptionEnum.PLATFORM_INVALID_REQUEST);
        }
        return normalized;
    }

    private String normalizeCode(String verificationCode) {
        String normalized = verificationCode == null ? "" : verificationCode.trim();
        if (!CODE_PATTERN.matcher(normalized).matches()) {
            throw new CustomException(ExceptionEnum.PLATFORM_PHONE_VERIFICATION_INVALID);
        }
        return normalized;
    }

    private String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String requireBoundPhone(CustomerUser customer) {
        String phone = normalizeNullable(customer.getPhone());
        if (phone == null) {
            throw new CustomException(ExceptionEnum.PLATFORM_PHONE_NOT_BOUND);
        }
        return normalizePhone(phone);
    }

    private String newVerificationCode() {
        return String.format(Locale.ROOT, "%06d", SECURE_RANDOM.nextInt(1_000_000));
    }

    private void insertBindingAudit(Long customerId, String phone, String clientIp) {
        String maskedPhone = maskPhone(phone);
        AuditLog auditLog = AuditLog.builder()
                .actorType(AuditActorType.CUSTOMER)
                .actorId(customerId)
                .action("CUSTOMER_PHONE_BOUND")
                .resourceType("CUSTOMER")
                .resourceNo(String.valueOf(customerId))
                .afterSnapshot("{\"maskedPhone\":\"" + maskedPhone + "\"}")
                .remark("客户通过短信验证码绑定手机号 " + maskedPhone)
                .clientIp(normalizeClientIp(clientIp))
                .build();
        if (auditLogMapper.insert(auditLog) != 1) {
            throw new IllegalStateException("客户手机号绑定审计写入失败");
        }
    }

    private String maskPhone(String phone) {
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    private String normalizeClientIp(String clientIp) {
        if (clientIp == null || clientIp.isBlank()) {
            return null;
        }
        String normalized = clientIp.trim();
        return normalized.length() <= 45 ? normalized : normalized.substring(0, 45);
    }

    private void invalidateAfterSendFailure(Long customerId, String phone) {
        try {
            verificationCodeStore.invalidate(customerId, phone);
        } catch (RuntimeException ex) {
            log.warn("验证码发送失败后清理 Redis 验证码失败，customerId={}", customerId, ex);
        }
    }
}
