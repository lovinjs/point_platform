package com.core.coreboot.platform.finance.mapper;

import com.core.coreboot.platform.finance.model.FinancialStoreAggregate;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

public interface FinancialReconciliationMapper {

    @Select("""
            <script>
            SELECT recharge_store_id AS store_id,
                   SUM(amount_cent) AS recharge_receipt_cent,
                   SUM(recharge_points) AS recharge_points
            FROM t_recharge_order
            WHERE completed_time &gt;= #{startTime}
              AND completed_time &lt; #{endTimeExclusive}
              AND order_status IN ('COMPLETED', 'REFUNDED')
            <if test="storeId != null">
              AND recharge_store_id = #{storeId}
            </if>
            GROUP BY recharge_store_id
            </script>
            """)
    List<FinancialStoreAggregate> selectRechargeAggregates(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTimeExclusive") LocalDateTime endTimeExclusive,
            @Param("storeId") Long storeId
    );

    @Select("""
            <script>
            SELECT o.recharge_store_id AS store_id,
                   SUM(r.refund_amount_cent) AS refund_outflow_cent,
                   SUM(r.refund_points) AS refund_points
            FROM t_recharge_refund r
            INNER JOIN t_recharge_order o ON o.id = r.recharge_order_id
            WHERE r.completed_time &gt;= #{startTime}
              AND r.completed_time &lt; #{endTimeExclusive}
              AND r.refund_status = 'COMPLETED'
            <if test="storeId != null">
              AND o.recharge_store_id = #{storeId}
            </if>
            GROUP BY o.recharge_store_id
            </script>
            """)
    List<FinancialStoreAggregate> selectRefundAggregates(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTimeExclusive") LocalDateTime endTimeExclusive,
            @Param("storeId") Long storeId
    );

    @Select("""
            <script>
            SELECT store_id,
                   SUM(gross_amount_cent) AS consumption_gross_cent,
                   SUM(consume_points) AS consumption_points,
                   SUM(platform_fee_cent) AS platform_fee_cent,
                   SUM(store_payable_cent) AS store_payable_cent,
                   SUM(CASE WHEN settlement_status = 'NOT_INCLUDED'
                            THEN store_payable_cent ELSE 0 END) AS not_included_payable_cent,
                   SUM(CASE WHEN settlement_status = 'INCLUDED'
                            THEN store_payable_cent ELSE 0 END) AS included_payable_cent,
                   SUM(CASE WHEN settlement_status IN ('SETTLED', 'ADJUSTED')
                            THEN store_payable_cent ELSE 0 END) AS settled_payable_cent
            FROM t_consumption_order
            WHERE completed_time &gt;= #{startTime}
              AND completed_time &lt; #{endTimeExclusive}
              AND order_status = 'COMPLETED'
            <if test="storeId != null">
              AND store_id = #{storeId}
            </if>
            GROUP BY store_id
            </script>
            """)
    List<FinancialStoreAggregate> selectConsumptionAggregates(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTimeExclusive") LocalDateTime endTimeExclusive,
            @Param("storeId") Long storeId
    );

    @Select("""
            <script>
            SELECT store_id,
                   SUM(payable_amount_cent) AS settlement_paid_cent
            FROM t_store_settlement
            WHERE paid_time &gt;= #{startTime}
              AND paid_time &lt; #{endTimeExclusive}
              AND settlement_status IN ('PAID', 'CLOSED')
            <if test="storeId != null">
              AND store_id = #{storeId}
            </if>
            GROUP BY store_id
            </script>
            """)
    List<FinancialStoreAggregate> selectSettlementPaymentAggregates(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTimeExclusive") LocalDateTime endTimeExclusive,
            @Param("storeId") Long storeId
    );
}
