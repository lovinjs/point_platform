package com.core.coreboot.platform.recharge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.core.coreboot.platform.recharge.entity.RechargeOrder;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface RechargeOrderMapper extends BaseMapper<RechargeOrder> {

    @Select("""
            SELECT id, order_no, customer_id, recharge_store_id, recharge_points, amount_cent,
                   channel, payment_method, fund_receiver, payment_reference, order_status,
                   operator_id, paid_time, completed_time, idempotency_key, remark,
                   create_time, update_time
            FROM t_recharge_order
            WHERE order_no = #{orderNo}
            LIMIT 1
            """)
    RechargeOrder selectByOrderNo(@Param("orderNo") String orderNo);

    @Select("""
            SELECT id, order_no, customer_id, recharge_store_id, recharge_points, amount_cent,
                   channel, payment_method, fund_receiver, payment_reference, order_status,
                   operator_id, paid_time, completed_time, idempotency_key, remark,
                   create_time, update_time
            FROM t_recharge_order
            WHERE order_no = #{orderNo}
            LIMIT 1
            FOR UPDATE
            """)
    RechargeOrder selectByOrderNoForUpdate(@Param("orderNo") String orderNo);

    @Update("""
            UPDATE t_recharge_order
            SET order_status = 'REFUNDED',
                update_time = CURRENT_TIMESTAMP
            WHERE id = #{orderId}
              AND order_status = 'COMPLETED'
            """)
    int markRefunded(@Param("orderId") Long orderId);
}
