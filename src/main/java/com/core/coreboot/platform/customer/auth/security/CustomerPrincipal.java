package com.core.coreboot.platform.customer.auth.security;

import com.core.coreboot.platform.common.enums.CustomerStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.List;

public final class CustomerPrincipal {
    private static final List<GrantedAuthority> AUTHORITIES =
            List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"));

    private final Long customerId;
    private final int tokenVersion;
    private final CustomerStatus status;

    public CustomerPrincipal(Long customerId, int tokenVersion, CustomerStatus status) {
        this.customerId = customerId;
        this.tokenVersion = tokenVersion;
        this.status = status;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public int getTokenVersion() {
        return tokenVersion;
    }

    public Collection<? extends GrantedAuthority> getAuthorities() {
        return AUTHORITIES;
    }

    public boolean isEnabled() {
        return status == CustomerStatus.ACTIVE;
    }
}
