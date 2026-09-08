package com.core.coreboot.platform.consumption.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.core.coreboot.platform.consumption.entity.ConsumptionOrder;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

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
}
