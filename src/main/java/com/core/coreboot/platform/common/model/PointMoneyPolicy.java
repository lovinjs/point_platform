package com.core.coreboot.platform.common.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Centralized V1 conversion rules for points, money and settlement fees.
 */
public final class PointMoneyPolicy {
    public static final long CENTS_PER_POINT = 100L;
    public static final int BASIS_POINTS_DIVISOR = 10_000;
    public static final int DEFAULT_PLATFORM_FEE_RATE_BPS = 500;

    private PointMoneyPolicy() {
    }

    public static long pointsFromRechargeAmount(long amountCent) {
        if (amountCent <= 0 || amountCent % CENTS_PER_POINT != 0) {
            throw new IllegalArgumentException("充值金额必须是大于0的整元金额");
        }
        return amountCent / CENTS_PER_POINT;
    }

    public static SettlementAmounts settlementAmounts(long consumePoints, int platformFeeRateBps) {
        if (consumePoints <= 0) {
            throw new IllegalArgumentException("消费积分必须大于0");
        }
        if (platformFeeRateBps < 0 || platformFeeRateBps > BASIS_POINTS_DIVISOR) {
            throw new IllegalArgumentException("平台手续费率必须在0到10000基点之间");
        }

        long grossAmountCent = Math.multiplyExact(consumePoints, CENTS_PER_POINT);
        long platformFeeCent = BigDecimal.valueOf(grossAmountCent)
                .multiply(BigDecimal.valueOf(platformFeeRateBps))
                .divide(BigDecimal.valueOf(BASIS_POINTS_DIVISOR), 0, RoundingMode.HALF_UP)
                .longValueExact();
        long storePayableCent = Math.subtractExact(grossAmountCent, platformFeeCent);
        return new SettlementAmounts(grossAmountCent, platformFeeCent, storePayableCent);
    }

    public record SettlementAmounts(
            long grossAmountCent,
            long platformFeeCent,
            long storePayableCent
    ) {
    }
}
