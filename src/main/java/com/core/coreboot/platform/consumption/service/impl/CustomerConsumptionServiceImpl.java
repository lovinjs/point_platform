package com.core.coreboot.platform.consumption.service.impl;

import com.core.coreboot.exception.CustomException;
import com.core.coreboot.exception.ExceptionEnum;
import com.core.coreboot.platform.common.enums.ConsumptionOrderStatus;
import com.core.coreboot.platform.consumption.entity.ConsumptionOrder;
import com.core.coreboot.platform.consumption.mapper.ConsumptionOrderMapper;
import com.core.coreboot.platform.consumption.model.ConsumptionConfirmationOutcome;
import com.core.coreboot.platform.consumption.model.ConsumptionConfirmationResult;
import com.core.coreboot.platform.consumption.model.CustomerPendingConsumptionView;
import com.core.coreboot.platform.consumption.service.CustomerConsumptionService;
import com.core.coreboot.platform.customer.service.CustomerConsumePinService;
import com.core.coreboot.platform.merchant.entity.Store;
import com.core.coreboot.platform.merchant.mapper.StoreMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;

@Service
public class CustomerConsumptionServiceImpl implements CustomerConsumptionService {
    private static final int ORDER_NO_MAX_LENGTH = 64;

    private final ConsumptionOrderMapper consumptionOrderMapper;
    private final StoreMapper storeMapper;
    private final CustomerConsumePinService customerConsumePinService;
    private final ConsumptionConfirmationTransactionService confirmationTransactionService;
    private final Clock clock;

    @Autowired
    public CustomerConsumptionServiceImpl(
            ConsumptionOrderMapper consumptionOrderMapper,
            StoreMapper storeMapper,
            CustomerConsumePinService customerConsumePinService,
            ConsumptionConfirmationTransactionService confirmationTransactionService
    ) {
        this(
                consumptionOrderMapper,
                storeMapper,
                customerConsumePinService,
                confirmationTransactionService,
                Clock.systemDefaultZone()
        );
    }

    CustomerConsumptionServiceImpl(
            ConsumptionOrderMapper consumptionOrderMapper,
            StoreMapper storeMapper,
            CustomerConsumePinService customerConsumePinService,
            ConsumptionConfirmationTransactionService confirmationTransactionService,
            Clock clock
    ) {
        this.consumptionOrderMapper = consumptionOrderMapper;
        this.storeMapper = storeMapper;
        this.customerConsumePinService = customerConsumePinService;
        this.confirmationTransactionService = confirmationTransactionService;
        this.clock = clock;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CustomerPendingConsumptionView getPending(Long customerId) {
        requireCustomerId(customerId);
        LocalDateTime now = LocalDateTime.now(clock);
        consumptionOrderMapper.expirePendingByCustomerId(customerId, now);
        ConsumptionOrder order = consumptionOrderMapper.selectActivePendingByCustomerId(customerId);
        if (order == null) {
            return null;
        }
        Store store = storeMapper.selectById(order.getStoreId());
        if (store == null) {
            throw new CustomException(ExceptionEnum.PLATFORM_STORE_NOT_FOUND);
        }
        return new CustomerPendingConsumptionView(
                order.getOrderNo(),
                order.getStoreId(),
                store.getStoreName(),
                requirePositive(order.getConsumePoints()),
                requireNonNegative(order.getGrossAmountCent()),
                order.getExpiresTime(),
                order.getOrderStatus(),
                order.getRemark(),
                order.getCreateTime()
        );
    }

    @Override
    public ConsumptionConfirmationResult confirm(
            Long customerId,
            String orderNo,
            String consumePin,
            String clientIp
    ) {
        requireCustomerId(customerId);
        String normalizedOrderNo = normalizeOrderNo(orderNo);
        ConsumptionOrder snapshot = consumptionOrderMapper.selectByOrderNo(normalizedOrderNo);
        requireOwnedOrder(snapshot, customerId);

        if (snapshot.getOrderStatus() == ConsumptionOrderStatus.EXPIRED) {
            throw new CustomException(ExceptionEnum.PLATFORM_CONSUMPTION_ORDER_EXPIRED);
        }
        if (snapshot.getOrderStatus() != ConsumptionOrderStatus.PENDING_CONFIRM
                && snapshot.getOrderStatus() != ConsumptionOrderStatus.COMPLETED) {
            throw new CustomException(ExceptionEnum.PLATFORM_CONSUMPTION_ORDER_NOT_PENDING);
        }

        if (snapshot.getOrderStatus() == ConsumptionOrderStatus.PENDING_CONFIRM) {
            LocalDateTime now = LocalDateTime.now(clock);
            if (snapshot.getExpiresTime() == null || !snapshot.getExpiresTime().isAfter(now)) {
                ConsumptionConfirmationOutcome outcome = confirmationTransactionService.confirmOrReplay(
                        customerId,
                        normalizedOrderNo,
                        clientIp
                );
                if (outcome.expired()) {
                    throw new CustomException(ExceptionEnum.PLATFORM_CONSUMPTION_ORDER_EXPIRED);
                }
                return requireResult(outcome);
            }
            customerConsumePinService.verifyPin(customerId, consumePin, clientIp);
        }

        ConsumptionConfirmationOutcome outcome = confirmationTransactionService.confirmOrReplay(
                customerId,
                normalizedOrderNo,
                clientIp
        );
        if (outcome.expired()) {
            throw new CustomException(ExceptionEnum.PLATFORM_CONSUMPTION_ORDER_EXPIRED);
        }
        return requireResult(outcome);
    }

    private ConsumptionConfirmationResult requireResult(ConsumptionConfirmationOutcome outcome) {
        if (outcome == null || outcome.result() == null) {
            throw new CustomException(ExceptionEnum.PLATFORM_CONSUMPTION_CONFIRM_FAILED);
        }
        return outcome.result();
    }

    private void requireOwnedOrder(ConsumptionOrder order, Long customerId) {
        if (order == null || !Objects.equals(order.getCustomerId(), customerId)) {
            throw new CustomException(ExceptionEnum.PLATFORM_CONSUMPTION_ORDER_NOT_FOUND);
        }
    }

    private void requireCustomerId(Long customerId) {
        if (customerId == null || customerId <= 0) {
            throw new CustomException(ExceptionEnum.PLATFORM_INVALID_REQUEST);
        }
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

    private long requirePositive(Long value) {
        if (value == null || value <= 0) {
            throw new CustomException(ExceptionEnum.PLATFORM_CONSUMPTION_CONFIRM_FAILED);
        }
        return value;
    }

    private long requireNonNegative(Long value) {
        if (value == null || value < 0) {
            throw new CustomException(ExceptionEnum.PLATFORM_CONSUMPTION_CONFIRM_FAILED);
        }
        return value;
    }
}
