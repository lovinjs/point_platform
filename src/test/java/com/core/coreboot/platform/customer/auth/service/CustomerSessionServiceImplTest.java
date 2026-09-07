package com.core.coreboot.platform.customer.auth.service;

import com.core.coreboot.platform.common.enums.CustomerStatus;
import com.core.coreboot.platform.customer.auth.model.CustomerAccountView;
import com.core.coreboot.platform.customer.auth.model.CustomerLoginTicket;
import com.core.coreboot.platform.customer.auth.model.CustomerSessionResponse;
import com.core.coreboot.platform.customer.auth.security.CustomerPrincipal;
import com.core.coreboot.platform.customer.auth.security.CustomerPrincipalService;
import com.core.coreboot.platform.customer.auth.security.CustomerTokenService;
import com.core.coreboot.platform.customer.mapper.CustomerUserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerSessionServiceImplTest {
    @Mock
    private CustomerLoginTicketStore loginTicketStore;
    @Mock
    private CustomerTokenService tokenService;
    @Mock
    private CustomerPrincipalService principalService;
    @Mock
    private CustomerProfileService profileService;
    @Mock
    private CustomerUserMapper customerUserMapper;

    private CustomerSessionServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CustomerSessionServiceImpl(
                loginTicketStore,
                tokenService,
                principalService,
                profileService,
                customerUserMapper
        );
    }

    @Test
    void shouldConsumeOneTimeTicketBeforeIssuingSession() {
        String ticket = "abcdefghijklmnopqrstuvwxyzABCDEFGH123456789";
        Instant expiresAt = Instant.parse("2026-09-06T08:00:00Z");
        CustomerPrincipal principal = new CustomerPrincipal(12L, 3, CustomerStatus.ACTIVE);
        CustomerAccountView account = new CustomerAccountView(
                12L, "客户", null, "138****8000", true, true, 50L
        );
        when(loginTicketStore.consume(ticket))
                .thenReturn(new CustomerLoginTicket(12L, "/pages/index/index"));
        when(principalService.loadByCustomerId(12L)).thenReturn(principal);
        when(tokenService.issue(principal))
                .thenReturn(new CustomerTokenService.IssuedCustomerToken("token", expiresAt, 7200));
        when(profileService.getProfile(12L)).thenReturn(account);

        CustomerSessionResponse result = service.exchangeLoginTicket(ticket);

        assertEquals("token", result.accessToken());
        assertEquals("Bearer", result.tokenType());
        assertEquals(50L, result.user().availablePoints());
        assertEquals("/pages/index/index", result.returnPath());
        verify(customerUserMapper).updateLastLoginTime(12L);
        var order = inOrder(tokenService, loginTicketStore);
        order.verify(tokenService).assertConfigured();
        order.verify(loginTicketStore).consume(ticket);
    }
}
