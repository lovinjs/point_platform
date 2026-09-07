package com.core.coreboot.platform.customer.service.impl;

import com.core.coreboot.exception.CustomException;
import com.core.coreboot.exception.ExceptionEnum;
import com.core.coreboot.platform.customer.entity.CustomerUser;
import com.core.coreboot.platform.customer.mapper.CustomerUserMapper;
import com.core.coreboot.platform.customer.model.AdminCustomerView;
import com.core.coreboot.platform.customer.service.AdminCustomerQueryService;
import com.core.coreboot.platform.point.entity.PointAccount;
import com.core.coreboot.platform.point.mapper.PointAccountMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class AdminCustomerQueryServiceImpl implements AdminCustomerQueryService {
    private static final Pattern PHONE_PATTERN = Pattern.compile("\\+?[0-9]{6,20}");

    private final CustomerUserMapper customerUserMapper;
    private final PointAccountMapper pointAccountMapper;

    @Override
    public AdminCustomerView findByPhone(String phone) {
        String normalizedPhone = normalizePhone(phone);
        CustomerUser customer = customerUserMapper.selectByPhone(normalizedPhone);
        if (customer == null) {
            throw new CustomException(ExceptionEnum.PLATFORM_CUSTOMER_NOT_FOUND);
        }

        PointAccount account = pointAccountMapper.selectByCustomerId(customer.getId());
        long availablePoints = account == null || account.getAvailablePoints() == null
                ? 0L
                : account.getAvailablePoints();
        if (availablePoints < 0) {
            throw new CustomException(ExceptionEnum.PLATFORM_POINT_ACCOUNT_ERROR);
        }
        return new AdminCustomerView(
                customer.getId(),
                customer.getPhone(),
                customer.getNickname(),
                customer.getStatus(),
                availablePoints
        );
    }

    private String normalizePhone(String phone) {
        if (phone == null || phone.isBlank()) {
            throw new CustomException(ExceptionEnum.PLATFORM_INVALID_REQUEST);
        }
        String normalized = phone.trim();
        if (!PHONE_PATTERN.matcher(normalized).matches()) {
            throw new CustomException(ExceptionEnum.PLATFORM_INVALID_REQUEST);
        }
        return normalized;
    }
}
