package com.core.coreboot.platform.customer.auth.security;

import com.core.coreboot.platform.customer.entity.CustomerUser;
import com.core.coreboot.platform.customer.mapper.CustomerUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomerPrincipalService {
    private final CustomerUserMapper customerUserMapper;

    public CustomerPrincipal loadByCustomerId(Long customerId) {
        if (customerId == null || customerId <= 0) {
            throw notFound();
        }
        CustomerUser customer = customerUserMapper.selectById(customerId);
        if (customer == null) {
            throw notFound();
        }
        return new CustomerPrincipal(
                customer.getId(),
                customer.getTokenVersion() == null ? 0 : customer.getTokenVersion(),
                customer.getStatus()
        );
    }

    private UsernameNotFoundException notFound() {
        return new UsernameNotFoundException("客户账号不存在");
    }
}
