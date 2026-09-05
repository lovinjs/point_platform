package com.core.coreboot.platform.common.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PointMoneyPolicyTest {

    @Test
    void shouldConvertWholeYuanRechargeAmountToPoints() {
        assertEquals(1L, PointMoneyPolicy.pointsFromRechargeAmount(100L));
        assertEquals(100L, PointMoneyPolicy.pointsFromRechargeAmount(10_000L));
    }

    @Test
    void shouldRejectNonPositiveOrNonWholeYuanRechargeAmount() {
        assertThrows(IllegalArgumentException.class, () -> PointMoneyPolicy.pointsFromRechargeAmount(0L));
        assertThrows(IllegalArgumentException.class, () -> PointMoneyPolicy.pointsFromRechargeAmount(199L));
    }

    @Test
    void shouldCalculateFivePercentPlatformFee() {
        PointMoneyPolicy.SettlementAmounts amounts = PointMoneyPolicy.settlementAmounts(
                100L,
                PointMoneyPolicy.DEFAULT_PLATFORM_FEE_RATE_BPS
        );

        assertEquals(10_000L, amounts.grossAmountCent());
        assertEquals(500L, amounts.platformFeeCent());
        assertEquals(9_500L, amounts.storePayableCent());
    }

    @Test
    void shouldRejectInvalidSettlementArguments() {
        assertThrows(IllegalArgumentException.class, () -> PointMoneyPolicy.settlementAmounts(0L, 500));
        assertThrows(IllegalArgumentException.class, () -> PointMoneyPolicy.settlementAmounts(1L, 10_001));
    }
}
