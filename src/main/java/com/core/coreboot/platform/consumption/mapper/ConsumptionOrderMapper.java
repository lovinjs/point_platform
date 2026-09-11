package com.core.coreboot.platform.consumption.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.core.coreboot.platform.consumption.entity.ConsumptionOrder;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

public interface ConsumptionOrderMapper extends BaseMapper<ConsumptionOrder> {

    @Select("""
            SELECT id, order_no, customer_id, store_id, consume_points, gross_amount_cent,
                   platform_fee_rate_bps, platform_fee_cent, store_payable_cent,
                   verification_mode, expires_time, order_status, operator_id,
                   confirmed_time, completed_time, reversed_by, reversed_time, reversal_reason,
                   settlement_status, idempotency_key, remark, create_time, update_time
            FROM t_consumption_order
            WHERE idempotency_key = #{idempotencyKey}
            LIMIT 1
            """)
    ConsumptionOrder selectByIdempotencyKey(@Param("idempotencyKey") String idempotencyKey);

    @Select("""
            SELECT id, order_no, customer_id, store_id, consume_points, gross_amount_cent,
                   platform_fee_rate_bps, platform_fee_cent, store_payable_cent,
                   verification_mode, expires_time, order_status, operator_id,
                   confirmed_time, completed_time, reversed_by, reversed_time, reversal_reason,
                   settlement_status, idempotency_key, remark, create_time, update_time
            FROM t_consumption_order
            WHERE order_no = #{orderNo}
            LIMIT 1
            """)
    ConsumptionOrder selectByOrderNo(@Param("orderNo") String orderNo);

    @Select("""
            SELECT id, order_no, customer_id, store_id, consume_points, gross_amount_cent,
                   platform_fee_rate_bps, platform_fee_cent, store_payable_cent,
                   verification_mode, expires_time, order_status, operator_id,
                   confirmed_time, completed_time, reversed_by, reversed_time, reversal_reason,
                   settlement_status, idempotency_key, remark, create_time, update_time
            FROM t_consumption_order
            WHERE order_no = #{orderNo}
            LIMIT 1
            FOR UPDATE
            """)
    ConsumptionOrder selectByOrderNoForUpdate(@Param("orderNo") String orderNo);

    @Select("""
            SELECT id, order_no, customer_id, store_id, consume_points, gross_amount_cent,
                   platform_fee_rate_bps, platform_fee_cent, store_payable_cent,
                   verification_mode, expires_time, order_status, operator_id,
                   confirmed_time, completed_time, reversed_by, reversed_time, reversal_reason,
                   settlement_status, idempotency_key, remark, create_time, update_time
            FROM t_consumption_order
            WHERE customer_id = #{customerId}
              AND order_status = 'PENDING_CONFIRM'
            LIMIT 1
            """)
    ConsumptionOrder selectActivePendingByCustomerId(@Param("customerId") Long customerId);

    @Update("""
            UPDATE t_consumption_order
            SET order_status = 'EXPIRED',
                update_time = CURRENT_TIMESTAMP
            WHERE customer_id = #{customerId}
              AND order_status = 'PENDING_CONFIRM'
              AND expires_time <= #{now}
            """)
    int expirePendingByCustomerId(
            @Param("customerId") Long customerId,
            @Param("now") LocalDateTime now
    );

    @Update("""
            UPDATE t_consumption_order
            SET order_status = 'EXPIRED',
                update_time = CURRENT_TIMESTAMP
            WHERE id = #{orderId}
              AND order_status = 'PENDING_CONFIRM'
              AND expires_time <= #{now}
            """)
    int expirePendingById(
            @Param("orderId") Long orderId,
            @Param("now") LocalDateTime now
    );

    @Update("""
            UPDATE t_consumption_order
            SET order_status = 'COMPLETED',
                confirmed_time = #{completedTime},
                completed_time = #{completedTime},
                update_time = CURRENT_TIMESTAMP
            WHERE id = #{orderId}
              AND order_status = 'PENDING_CONFIRM'
            """)
    int completePendingById(
            @Param("orderId") Long orderId,
            @Param("completedTime") LocalDateTime completedTime
    );

    @Update("""
            UPDATE t_consumption_order
            SET order_status = 'CANCELLED',
                update_time = #{cancelledTime}
            WHERE customer_id = #{customerId}
              AND order_status = 'PENDING_CONFIRM'
            """)
    int cancelPendingByCustomerId(
            @Param("customerId") Long customerId,
            @Param("cancelledTime") LocalDateTime cancelledTime
    );

    @Select("""
            SELECT id, order_no, customer_id, store_id, consume_points, gross_amount_cent,
                   platform_fee_rate_bps, platform_fee_cent, store_payable_cent,
                   verification_mode, expires_time, order_status, operator_id,
                   confirmed_time, completed_time, reversed_by, reversed_time, reversal_reason,
                   settlement_status, idempotency_key, remark, create_time, update_time
            FROM t_consumption_order
            WHERE order_status = 'COMPLETED'
              AND settlement_status = 'NOT_INCLUDED'
              AND completed_time >= #{startTime}
              AND completed_time < #{endTimeExclusive}
            ORDER BY store_id, completed_time, id
            FOR UPDATE
            """)
    List<ConsumptionOrder> selectEligibleForSettlement(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTimeExclusive") LocalDateTime endTimeExclusive
    );

    @Update("""
            UPDATE t_consumption_order
            SET settlement_status = 'INCLUDED',
                update_time = CURRENT_TIMESTAMP
            WHERE id = #{orderId}
              AND order_status = 'COMPLETED'
              AND settlement_status = 'NOT_INCLUDED'
            """)
    int markIncludedInSettlement(@Param("orderId") Long orderId);

    @Update("""
            UPDATE t_consumption_order
            SET order_status = 'REVERSED',
                reversed_by = #{operatorId},
                reversed_time = #{reversedTime},
                reversal_reason = #{reason},
                settlement_status = CASE
                    WHEN settlement_status = 'NOT_INCLUDED' THEN 'ADJUSTED'
                    ELSE settlement_status
                END,
                update_time = #{reversedTime}
            WHERE id = #{orderId}
              AND order_status = 'COMPLETED'
              AND settlement_status IN ('NOT_INCLUDED', 'INCLUDED', 'SETTLED')
            """)
    int markReversed(
            @Param("orderId") Long orderId,
            @Param("operatorId") Long operatorId,
            @Param("reversedTime") LocalDateTime reversedTime,
            @Param("reason") String reason
    );

    @Select("""
            SELECT id, order_no, customer_id, store_id, consume_points, gross_amount_cent,
                   platform_fee_rate_bps, platform_fee_cent, store_payable_cent,
                   verification_mode, expires_time, order_status, operator_id,
                   confirmed_time, completed_time, reversed_by, reversed_time, reversal_reason,
                   settlement_status, idempotency_key, remark, create_time, update_time
            FROM t_consumption_order
            WHERE order_status = 'REVERSED'
              AND settlement_status = 'SETTLED'
              AND reversed_time < #{endTimeExclusive}
            ORDER BY store_id, reversed_time, id
            FOR UPDATE
            """)
    List<ConsumptionOrder> selectEligibleReversalsForSettlement(
            @Param("endTimeExclusive") LocalDateTime endTimeExclusive
    );

    @Update("""
            UPDATE t_consumption_order
            SET settlement_status = 'ADJUSTED',
                update_time = CURRENT_TIMESTAMP
            WHERE id = #{orderId}
              AND order_status = 'REVERSED'
              AND settlement_status = 'SETTLED'
            """)
    int markReversalAdjusted(@Param("orderId") Long orderId);

    @Update("""
            UPDATE t_consumption_order o
            INNER JOIN t_store_settlement_item i
                ON i.consumption_order_id = o.id
               AND i.item_type = 'CONSUMPTION'
            SET o.settlement_status = 'SETTLED',
                o.update_time = CURRENT_TIMESTAMP
            WHERE i.settlement_id = #{settlementId}
              AND i.item_type = 'CONSUMPTION'
              AND o.order_status IN ('COMPLETED', 'REVERSED')
              AND o.settlement_status = 'INCLUDED'
            """)
    int markSettlementItemsSettled(@Param("settlementId") Long settlementId);
}
