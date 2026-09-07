package com.core.coreboot.platform.customer.phone.service;

import com.core.coreboot.exception.CustomException;
import com.core.coreboot.platform.audit.entity.AuditLog;
import com.core.coreboot.platform.audit.mapper.AuditLogMapper;
import com.core.coreboot.platform.common.enums.CustomerStatus;
import com.core.coreboot.platform.customer.auth.model.CustomerAccountView;
import com.core.coreboot.platform.customer.auth.service.CustomerProfileService;
import com.core.coreboot.platform.customer.entity.CustomerUser;
import com.core.coreboot.platform.customer.mapper.CustomerUserMapper;
import com.core.coreboot.platform.customer.phone.config.PhoneVerificationProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerPhoneServiceImplTest {
    private static final Long CUSTOMER_ID = 12L;
    private static final String PHONE = "13800138000";

    @Mock
    private CustomerUserMapper customerUserMapper;
    @Mock
    private AuditLogMapper auditLogMapper;
    @Mock
    private CustomerProfileService customerProfileService;
    @Mock
    private PhoneVerificationCodeStore verificationCodeStore;
    @Mock
    private PhoneVerificationSender verificationSender;

    private PhoneVerificationProperties properties;
    private CustomerPhoneServiceImpl service;

    @BeforeEach
    void setUp() {
        properties = new PhoneVerificationProperties();
        service = new CustomerPhoneServiceImpl(
                customerUserMapper,
                auditLogMapper,
                customerProfileService,
                verificationCodeStore,
                verificationSender,
                properties
        );
    }

    @Test
    void shouldSendShortLivedCodeAfterReservingLimits() {
        when(customerUserMapper.selectById(CUSTOMER_ID)).thenReturn(activeCustomer(null));

        var result = service.requestVerificationCode(CUSTOMER_ID, " 13800138000 ");

        assertEquals(300L, result.expiresInSeconds());
        assertEquals(60L, result.resendAfterSeconds());
        verify(verificationSender).validateReady();
        verify(verificationCodeStore).reserveSend(CUSTOMER_ID, PHONE, properties);

        ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
        verify(verificationCodeStore).saveCode(
                org.mockito.ArgumentMatchers.eq(CUSTOMER_ID),
                org.mockito.ArgumentMatchers.eq(PHONE),
                codeCaptor.capture(),
                org.mockito.ArgumentMatchers.eq(properties.getCodeTtl())
        );
        assertTrue(codeCaptor.getValue().matches("\\d{6}"));
        verify(verificationSender).send(PHONE, codeCaptor.getValue(), properties.getCodeTtl());
    }

    @Test
    void shouldBindVerifiedPhoneAndWriteMaskedAudit() {
        CustomerAccountView profile = new CustomerAccountView(
                CUSTOMER_ID, "测试用户", null, "138****8000", true, false, 0L
        );
        when(customerUserMapper.selectByIdForUpdate(CUSTOMER_ID)).thenReturn(activeCustomer(null));
        when(customerUserMapper.selectByPhoneForUpdate(PHONE)).thenReturn(null);
        when(customerUserMapper.bindPhoneIfUnbound(CUSTOMER_ID, PHONE)).thenReturn(1);
        when(auditLogMapper.insert(any(AuditLog.class))).thenReturn(1);
        when(customerProfileService.getProfile(CUSTOMER_ID)).thenReturn(profile);

        CustomerAccountView result = service.bindPhone(
                CUSTOMER_ID, PHONE, "825391", "127.0.0.1"
        );

        assertSame(profile, result);
        verify(verificationCodeStore).verifyAndConsume(CUSTOMER_ID, PHONE, "825391", 5);
        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogMapper).insert(auditCaptor.capture());
        assertEquals("CUSTOMER_PHONE_BOUND", auditCaptor.getValue().getAction());
        assertEquals("{\"maskedPhone\":\"138****8000\"}", auditCaptor.getValue().getAfterSnapshot());
    }

    @Test
    void shouldNotMergeWhenPhoneBelongsToAnotherCustomer() {
        when(customerUserMapper.selectByIdForUpdate(CUSTOMER_ID)).thenReturn(activeCustomer(null));
        CustomerUser otherCustomer = activeCustomer(PHONE);
        otherCustomer.setId(99L);
        when(customerUserMapper.selectByPhoneForUpdate(PHONE)).thenReturn(otherCustomer);

        CustomException exception = assertThrows(
                CustomException.class,
                () -> service.bindPhone(CUSTOMER_ID, PHONE, "825391", null)
        );

        assertEquals(30048, exception.getCode());
        verify(verificationCodeStore).verifyAndConsume(CUSTOMER_ID, PHONE, "825391", 5);
        verify(customerUserMapper, never()).bindPhoneIfUnbound(any(), any());
        verify(auditLogMapper, never()).insert(any(AuditLog.class));
    }

    @Test
    void shouldTreatBindingTheSamePhoneAsIdempotent() {
        CustomerAccountView profile = new CustomerAccountView(
                CUSTOMER_ID, null, null, "138****8000", true, false, 0L
        );
        when(customerUserMapper.selectByIdForUpdate(CUSTOMER_ID)).thenReturn(activeCustomer(PHONE));
        when(customerProfileService.getProfile(CUSTOMER_ID)).thenReturn(profile);

        CustomerAccountView result = service.bindPhone(
                CUSTOMER_ID, PHONE, "000000", null
        );

        assertSame(profile, result);
        verify(verificationCodeStore, never()).verifyAndConsume(any(), any(), any(), any(Integer.class));
        verify(customerUserMapper, never()).bindPhoneIfUnbound(any(), any());
    }

    private CustomerUser activeCustomer(String phone) {
        return CustomerUser.builder()
                .id(CUSTOMER_ID)
                .phone(phone)
                .status(CustomerStatus.ACTIVE)
                .tokenVersion(0)
                .build();
    }
}
