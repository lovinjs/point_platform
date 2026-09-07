package com.core.coreboot.platform.customer.auth.service;

import com.core.coreboot.exception.CustomException;
import com.core.coreboot.exception.ExceptionEnum;
import com.core.coreboot.platform.customer.auth.model.CustomerAccountView;
import com.core.coreboot.platform.customer.auth.model.CustomerLoginTicket;
import com.core.coreboot.platform.customer.auth.model.CustomerSessionResponse;
import com.core.coreboot.platform.customer.auth.security.CustomerPrincipal;
import com.core.coreboot.platform.customer.auth.security.CustomerPrincipalService;
import com.core.coreboot.platform.customer.auth.security.CustomerTokenService;
import com.core.coreboot.platform.customer.mapper.CustomerUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomerSessionServiceImpl implements CustomerSessionService {
    private final CustomerLoginTicketStore loginTicketStore;
    private final CustomerTokenService tokenService;
    private final CustomerPrincipalService principalService;
    private final CustomerProfileService profileService;
    private final CustomerUserMapper customerUserMapper;

    @Override
    public CustomerSessionResponse exchangeLoginTicket(String ticket) {
        tokenService.assertConfigured();
        CustomerLoginTicket loginTicket = loginTicketStore.consume(ticket);
        CustomerPrincipal principal = principalService.loadByCustomerId(loginTicket.customerId());
        if (!principal.isEnabled()) {
            throw new CustomException(ExceptionEnum.PLATFORM_CUSTOMER_DISABLED);
        }

        CustomerTokenService.IssuedCustomerToken issuedToken = tokenService.issue(principal);
        CustomerAccountView account = profileService.getProfile(principal.getCustomerId());
        customerUserMapper.updateLastLoginTime(principal.getCustomerId());
        return new CustomerSessionResponse(
                issuedToken.token(),
                "Bearer",
                issuedToken.expiresInSeconds(),
                issuedToken.expiresAt(),
                account,
                loginTicket.returnPath()
        );
    }
}
