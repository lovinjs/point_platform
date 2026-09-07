package com.core.coreboot.platform.customer.auth.service;

import com.core.coreboot.exception.CustomException;
import com.core.coreboot.exception.ExceptionEnum;
import com.core.coreboot.platform.common.enums.CustomerStatus;
import com.core.coreboot.platform.customer.auth.model.CustomerAccountView;
import com.core.coreboot.platform.customer.entity.CustomerSecurity;
import com.core.coreboot.platform.customer.entity.CustomerUser;
import com.core.coreboot.platform.customer.mapper.CustomerSecurityMapper;
import com.core.coreboot.platform.customer.mapper.CustomerUserMapper;
import com.core.coreboot.platform.point.entity.PointAccount;
import com.core.coreboot.platform.point.mapper.PointAccountMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomerProfileServiceImpl implements CustomerProfileService {
    private final CustomerUserMapper customerUserMapper;
    private final CustomerSecurityMapper customerSecurityMapper;
    private final PointAccountMapper pointAccountMapper;

    @Override
    public CustomerAccountView getProfile(Long customerId) {
        if (customerId == null || customerId <= 0) {
            throw new CustomException(ExceptionEnum.PLATFORM_CUSTOMER_NOT_FOUND);
        }
        CustomerUser customer = customerUserMapper.selectById(customerId);
        if (customer == null) {
            throw new CustomException(ExceptionEnum.PLATFORM_CUSTOMER_NOT_FOUND);
        }
        if (customer.getStatus() != CustomerStatus.ACTIVE) {
            throw new CustomException(ExceptionEnum.PLATFORM_CUSTOMER_DISABLED);
        }

        CustomerSecurity security = customerSecurityMapper.selectById(customerId);
        PointAccount pointAccount = pointAccountMapper.selectByCustomerId(customerId);
        String phone = normalizeNullable(customer.getPhone());
        boolean consumePinConfigured = security != null
                && security.getConsumePinHash() != null
                && !security.getConsumePinHash().isBlank();

        return new CustomerAccountView(
                customer.getId(),
                normalizeNullable(customer.getNickname()),
                normalizeNullable(customer.getAvatarUrl()),
                maskPhone(phone),
                phone != null,
                consumePinConfigured,
                pointAccount == null || pointAccount.getAvailablePoints() == null
                        ? 0L
                        : pointAccount.getAvailablePoints()
        );
    }

    private String normalizeNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String maskPhone(String phone) {
        if (phone == null) {
            return null;
        }
        if (phone.length() < 7) {
            return "****";
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
