package com.core.coreboot.platform.consumption.service.impl;

import com.core.coreboot.exception.CustomException;
import com.core.coreboot.exception.ExceptionEnum;
import com.core.coreboot.platform.common.enums.ConsumptionOrderStatus;
import com.core.coreboot.platform.consumption.entity.ConsumptionOrder;
import com.core.coreboot.platform.consumption.mapper.ConsumptionOrderMapper;
import com.core.coreboot.platform.consumption.model.AdminConsumptionOrderStatusView;
import com.core.coreboot.platform.consumption.service.AdminConsumptionOrderQueryService;
import com.core.coreboot.platform.staff.service.StaffStoreAuthorizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
public class AdminConsumptionOrderQueryServiceImpl implements AdminConsumptionOrderQueryService {
    private static final int ORDER_NO_MAX_LENGTH = 64;

    private final ConsumptionOrderMapper consumptionOrderMapper;
    private final StaffStoreAuthorizationService staffStoreAuthorizationService;
    private final Clock clock;

    @Autowired
    public AdminConsumptionOrderQueryServiceImpl(
            ConsumptionOrderMapper consumptionOrderMapper,
            StaffStoreAuthorizationService staffStoreAuthorizationService
    ) {
        this(consumptionOrderMapper, staffStoreAuthorizationService, Clock.systemDefaultZone());
    }

    AdminConsumptionOrderQueryServiceImpl(
            ConsumptionOrderMapper consumptionOrderMapper,
            StaffStoreAuthorizationService staffStoreAuthorizationService,
            Clock clock
    ) {
        this.consumptionOrderMapper = consumptionOrderMapper;
        this.staffStoreAuthorizationService = staffStoreAuthorizationService;
        this.clock = clock;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminConsumptionOrderStatusView getStatus(Long operatorId, String orderNo) {
        String normalizedOrderNo = normalizeOrderNo(orderNo);
        ConsumptionOrder order = consumptionOrderMapper.selectByOrderNo(normalizedOrderNo);
        if (order == null) {
            throw new CustomException(ExceptionEnum.PLATFORM_CONSUMPTION_ORDER_NOT_FOUND);
        }
        staffStoreAuthorizationService.requireActiveStoreAccess(operatorId, order.getStoreId());

        LocalDateTime now = LocalDateTime.now(clock);
        if (order.getOrderStatus() == ConsumptionOrderStatus.PENDING_CONFIRM
                && order.getExpiresTime() == null) {
            throw new CustomException(ExceptionEnum.PLATFORM_CONSUMPTION_CONFIRM_FAILED);
        }
        if (order.getOrderStatus() == ConsumptionOrderStatus.PENDING_CONFIRM
                && !order.getExpiresTime().isAfter(now)) {
            consumptionOrderMapper.expirePendingById(order.getId(), now);
            ConsumptionOrder refreshed = consumptionOrderMapper.selectByOrderNo(normalizedOrderNo);
            if (refreshed == null) {
                throw new CustomException(ExceptionEnum.PLATFORM_CONSUMPTION_ORDER_NOT_FOUND);
            }
            order = refreshed;
        }
        return toView(order);
    }

    private String normalizeOrderNo(String orderNo) {
        if (orderNo == null || orderNo.isBlank()) {
            throw new CustomException(ExceptionEnum.PLATFORM_INVALID_REQUEST);
        }
        String normalized = orderNo.trim();
        if (normalized.length() > ORDER_NO_MAX_LENGTH) {
            throw new CustomException(ExceptionEnum.PLATFORM_INVALID_REQUEST);
        }
        return normalized;
    }

    private AdminConsumptionOrderStatusView toView(ConsumptionOrder order) {
        if (order.getConsumePoints() == null || order.getConsumePoints() <= 0
                || order.getGrossAmountCent() == null || order.getGrossAmountCent() < 0
                || order.getOrderStatus() == null || order.getVerificationMode() == null) {
            throw new CustomException(ExceptionEnum.PLATFORM_CONSUMPTION_CONFIRM_FAILED);
        }
        return new AdminConsumptionOrderStatusView(
                order.getOrderNo(),
                order.getCustomerId(),
                order.getStoreId(),
                order.getConsumePoints(),
                order.getGrossAmountCent(),
                order.getVerificationMode(),
                order.getOrderStatus(),
                order.getRemark(),
                order.getExpiresTime(),
                order.getConfirmedTime(),
                order.getCompletedTime(),
                order.getCreateTime()
        );
    }
}
