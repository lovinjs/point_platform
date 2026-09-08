package com.core.coreboot.platform.recharge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.core.coreboot.platform.common.enums.RefundMethod;
import com.core.coreboot.platform.recharge.entity.RechargeRefund;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface RechargeRefundMapper extends BaseMapper<RechargeRefund> {

    @Select("""
            SELECT id, refund_no, recharge_order_id, customer_id, refund_points,
                   refund_amount_cent, refund_method, refund_reference, refund_status,
                   operator_id, reason, completed_time, idempotency_key, create_time, update_time
            FROM t_recharge_refund
            WHERE idempotency_key = #{idempotencyKey}
            LIMIT 1
            """)
    RechargeRefund selectByIdempotencyKey(@Param("idempotencyKey") String idempotencyKey);

    @Select("""
            SELECT id, refund_no, recharge_order_id, customer_id, refund_points,
                   refund_amount_cent, refund_method, refund_reference, refund_status,
                   operator_id, reason, completed_time, idempotency_key, create_time, update_time
            FROM t_recharge_refund
            WHERE recharge_order_id = #{rechargeOrderId}
            LIMIT 1
            """)
    RechargeRefund selectByRechargeOrderId(@Param("rechargeOrderId") Long rechargeOrderId);

    @Select("""
            SELECT COUNT(1)
            FROM t_recharge_refund
            WHERE refund_method = #{refundMethod}
              AND refund_reference = #{refundReference}
            """)
    long countByRefundReference(
            @Param("refundMethod") RefundMethod refundMethod,
            @Param("refundReference") String refundReference
    );
}
