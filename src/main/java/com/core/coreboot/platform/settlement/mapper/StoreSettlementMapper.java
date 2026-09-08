package com.core.coreboot.platform.settlement.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.core.coreboot.platform.settlement.entity.StoreSettlement;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

public interface StoreSettlementMapper extends BaseMapper<StoreSettlement> {

    @Select("""
            SELECT id, settlement_no, period_id, merchant_id, store_id,
                   total_consume_points, gross_amount_cent, platform_fee_cent,
                   adjustment_amount_cent, payable_amount_cent, settlement_status,
                   confirmed_time, paid_time, payment_reference, remark,
                   create_time, update_time
            FROM t_store_settlement
            WHERE settlement_no = #{settlementNo}
            LIMIT 1
            """)
    StoreSettlement selectBySettlementNo(@Param("settlementNo") String settlementNo);

    @Select("""
            SELECT id, settlement_no, period_id, merchant_id, store_id,
                   total_consume_points, gross_amount_cent, platform_fee_cent,
                   adjustment_amount_cent, payable_amount_cent, settlement_status,
                   confirmed_time, paid_time, payment_reference, remark,
                   create_time, update_time
            FROM t_store_settlement
            WHERE settlement_no = #{settlementNo}
            LIMIT 1
            FOR UPDATE
            """)
    StoreSettlement selectBySettlementNoForUpdate(@Param("settlementNo") String settlementNo);

    @Select("""
            SELECT id, settlement_no, period_id, merchant_id, store_id,
                   total_consume_points, gross_amount_cent, platform_fee_cent,
                   adjustment_amount_cent, payable_amount_cent, settlement_status,
                   confirmed_time, paid_time, payment_reference, remark,
                   create_time, update_time
            FROM t_store_settlement
            WHERE period_id = #{periodId}
            ORDER BY store_id, id
            """)
    List<StoreSettlement> selectByPeriodId(@Param("periodId") Long periodId);

    @Select("""
            SELECT COUNT(*)
            FROM t_store_settlement
            WHERE payment_reference = #{paymentReference}
              AND id <> #{settlementId}
            """)
    int countByPaymentReferenceExcluding(
            @Param("paymentReference") String paymentReference,
            @Param("settlementId") Long settlementId
    );

    @Update("""
            UPDATE t_store_settlement
            SET settlement_status = 'CONFIRMED',
                confirmed_time = #{confirmedTime},
                remark = COALESCE(#{remark}, remark),
                update_time = CURRENT_TIMESTAMP
            WHERE id = #{settlementId}
              AND settlement_status = 'GENERATED'
            """)
    int markConfirmed(
            @Param("settlementId") Long settlementId,
            @Param("confirmedTime") LocalDateTime confirmedTime,
            @Param("remark") String remark
    );

    @Update("""
            UPDATE t_store_settlement
            SET settlement_status = 'PAID',
                paid_time = #{paidTime},
                payment_reference = #{paymentReference},
                remark = COALESCE(#{remark}, remark),
                update_time = CURRENT_TIMESTAMP
            WHERE id = #{settlementId}
              AND settlement_status = 'CONFIRMED'
            """)
    int markPaid(
            @Param("settlementId") Long settlementId,
            @Param("paidTime") LocalDateTime paidTime,
            @Param("paymentReference") String paymentReference,
            @Param("remark") String remark
    );
}
