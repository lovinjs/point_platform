package com.core.coreboot.platform.customer.auth.service;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WechatCustomerIdentityServiceImplTest {
    @Mock
    private CustomerIdentityMapper customerIdentityMapper;
    @Mock
    private CustomerUserMapper customerUserMapper;
    @Mock
    private PointAccountMapper pointAccountMapper;
    @Mock
    private AuditLogMapper auditLogMapper;

    private WechatCustomerIdentityServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new WechatCustomerIdentityServiceImpl(
                customerIdentityMapper,
                customerUserMapper,
                pointAccountMapper,
                auditLogMapper
        );
    }

    @Test
    void shouldCreateProvisionalCustomerOnFirstWechatLogin() {
        WechatUserProfile profile = new WechatUserProfile(
                "open-id", "微信用户", "https://example.test/avatar.jpg", "union-id"
        );
        when(customerIdentityMapper.selectByExternalIdentity("WECHAT_H5", "wx-test-app", "open-id"))
                .thenReturn(null);
        doAnswer(invocation -> {
            CustomerUser customer = invocation.getArgument(0);
            customer.setId(21L);
            return 1;
        }).when(customerUserMapper).insert(any(CustomerUser.class));
        when(pointAccountMapper.ensureAccount(21L)).thenReturn(1);
        when(customerIdentityMapper.insertIgnore(any(CustomerIdentity.class))).thenReturn(1);
        when(auditLogMapper.insert(any(AuditLog.class))).thenReturn(1);

        Long customerId = service.resolveCustomer("wx-test-app", profile);

        assertEquals(21L, customerId);
        ArgumentCaptor<CustomerUser> customerCaptor = ArgumentCaptor.forClass(CustomerUser.class);
        verify(customerUserMapper).insert(customerCaptor.capture());
        CustomerUser customer = customerCaptor.getValue();
        assertNull(customer.getPhone());
        assertEquals(CustomerStatus.ACTIVE, customer.getStatus());
        assertEquals(0, customer.getTokenVersion());
        assertEquals("微信用户", customer.getNickname());

        ArgumentCaptor<CustomerIdentity> identityCaptor = ArgumentCaptor.forClass(CustomerIdentity.class);
        verify(customerIdentityMapper).insertIgnore(identityCaptor.capture());
        CustomerIdentity identity = identityCaptor.getValue();
        assertEquals(21L, identity.getCustomerId());
        assertEquals("WECHAT_H5", identity.getProviderCode());
        assertEquals("wx-test-app", identity.getAppId());
        assertEquals("open-id", identity.getExternalId());
        assertEquals("union-id", identity.getUnionId());

        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogMapper).insert(auditCaptor.capture());
        assertEquals(AuditActorType.CUSTOMER, auditCaptor.getValue().getActorType());
        assertEquals("CUSTOMER_WECHAT_H5_REGISTERED", auditCaptor.getValue().getAction());
    }

    @Test
    void shouldReuseExistingWechatIdentity() {
        CustomerIdentity identity = CustomerIdentity.builder()
                .id(5L)
                .customerId(21L)
                .providerCode("WECHAT_H5")
                .appId("wx-test-app")
                .externalId("open-id")
                .unionId("union-id")
                .build();
        CustomerUser customer = CustomerUser.builder()
                .id(21L)
                .nickname("旧昵称")
                .status(CustomerStatus.ACTIVE)
                .tokenVersion(0)
                .build();
        WechatUserProfile profile = new WechatUserProfile(
                "open-id", "新昵称", "https://example.test/new-avatar.jpg", "union-id"
        );
        when(customerIdentityMapper.selectByExternalIdentity("WECHAT_H5", "wx-test-app", "open-id"))
                .thenReturn(identity);
        when(customerUserMapper.selectById(21L)).thenReturn(customer);
        when(customerUserMapper.updateWechatProfile(
                21L, "新昵称", "https://example.test/new-avatar.jpg"
        )).thenReturn(1);

        Long customerId = service.resolveCustomer("wx-test-app", profile);

        assertEquals(21L, customerId);
        verify(customerUserMapper).updateWechatProfile(
                21L, "新昵称", "https://example.test/new-avatar.jpg"
        );
        verify(customerUserMapper, never()).insert(any(CustomerUser.class));
        verify(pointAccountMapper, never()).ensureAccount(any());
        verify(auditLogMapper, never()).insert(any(AuditLog.class));
    }
}
