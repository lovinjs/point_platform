package com.core.coreboot.platform.dashboard.mapper;

import com.core.coreboot.platform.dashboard.model.DashboardBacklogAggregate;
import com.core.coreboot.platform.dashboard.model.DashboardDailyAggregate;
import com.core.coreboot.platform.dashboard.model.DashboardPeriodAggregate;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

public interface DashboardOverviewMapper {

    @Select("""
            <script>
            SELECT COALESCE(SUM(recharge_order_count), 0) AS recharge_order_count,
                   COALESCE(SUM(recharge_receipt_cent), 0) AS recharge_receipt_cent,
                   COALESCE(SUM(recharge_points), 0) AS recharge_points,
                   COALESCE(SUM(refund_order_count), 0) AS refund_order_count,
                   COALESCE(SUM(refund_outflow_cent), 0) AS refund_outflow_cent,
                   COALESCE(SUM(refund_points), 0) AS refund_points,
                   COALESCE(SUM(consumption_order_count), 0) AS consumption_order_count,
                   COALESCE(SUM(consumption_gross_cent), 0) AS consumption_gross_cent,
                   COALESCE(SUM(consumption_points), 0) AS consumption_points,
                   COALESCE(SUM(platform_fee_cent), 0) AS platform_fee_cent,
                   COALESCE(SUM(store_payable_cent), 0) AS store_payable_cent
            FROM (
                SELECT COUNT(*) AS recharge_order_count,
                       COALESCE(SUM(amount_cent), 0) AS recharge_receipt_cent,
                       COALESCE(SUM(recharge_points), 0) AS recharge_points,
                       0 AS refund_order_count,
                       0 AS refund_outflow_cent,
                       0 AS refund_points,
                       0 AS consumption_order_count,
                       0 AS consumption_gross_cent,
                       0 AS consumption_points,
                       0 AS platform_fee_cent,
                       0 AS store_payable_cent
                FROM t_recharge_order
                WHERE completed_time &gt;= #{startTime}
                  AND completed_time &lt; #{endTimeExclusive}
                  AND order_status IN ('COMPLETED', 'REFUNDED')
                <choose>
                  <when test="storeId != null">
                    AND recharge_store_id = #{storeId}
                  </when>
                  <when test="storeIds != null and storeIds.size() &gt; 0">
                    AND recharge_store_id IN
                    <foreach collection="storeIds" item="scopeStoreId" open="(" separator="," close=")">
                      #{scopeStoreId}
                    </foreach>
                  </when>
                </choose>

                UNION ALL

                SELECT 0, 0, 0,
                       COUNT(*),
                       COALESCE(SUM(r.refund_amount_cent), 0),
                       COALESCE(SUM(r.refund_points), 0),
                       0, 0, 0, 0, 0
                FROM t_recharge_refund r
                INNER JOIN t_recharge_order o ON o.id = r.recharge_order_id
                WHERE r.completed_time &gt;= #{startTime}
                  AND r.completed_time &lt; #{endTimeExclusive}
                  AND r.refund_status = 'COMPLETED'
                <choose>
                  <when test="storeId != null">
                    AND o.recharge_store_id = #{storeId}
                  </when>
                  <when test="storeIds != null and storeIds.size() &gt; 0">
                    AND o.recharge_store_id IN
                    <foreach collection="storeIds" item="scopeStoreId" open="(" separator="," close=")">
                      #{scopeStoreId}
                    </foreach>
                  </when>
                </choose>

                UNION ALL

                SELECT 0, 0, 0, 0, 0, 0,
                       COUNT(*),
                       COALESCE(SUM(gross_amount_cent), 0),
                       COALESCE(SUM(consume_points), 0),
                       COALESCE(SUM(platform_fee_cent), 0),
                       COALESCE(SUM(store_payable_cent), 0)
                FROM t_consumption_order
                WHERE completed_time &gt;= #{startTime}
                  AND completed_time &lt; #{endTimeExclusive}
                  AND order_status = 'COMPLETED'
                <choose>
                  <when test="storeId != null">
                    AND store_id = #{storeId}
                  </when>
                  <when test="storeIds != null and storeIds.size() &gt; 0">
                    AND store_id IN
                    <foreach collection="storeIds" item="scopeStoreId" open="(" separator="," close=")">
                      #{scopeStoreId}
                    </foreach>
                  </when>
                </choose>
            ) period_metrics
            </script>
            """)
    DashboardPeriodAggregate selectPeriodMetrics(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTimeExclusive") LocalDateTime endTimeExclusive,
            @Param("storeId") Long storeId,
            @Param("storeIds") List<Long> storeIds
    );

    @Select("""
            <script>
            SELECT business_date,
                   SUM(recharge_receipt_cent) AS recharge_receipt_cent,
                   SUM(refund_outflow_cent) AS refund_outflow_cent,
                   SUM(consumption_gross_cent) AS consumption_gross_cent,
                   SUM(platform_fee_cent) AS platform_fee_cent,
                   SUM(store_payable_cent) AS store_payable_cent
            FROM (
                SELECT DATE(completed_time) AS business_date,
                       SUM(amount_cent) AS recharge_receipt_cent,
                       0 AS refund_outflow_cent,
                       0 AS consumption_gross_cent,
                       0 AS platform_fee_cent,
                       0 AS store_payable_cent
                FROM t_recharge_order
                WHERE completed_time &gt;= #{startTime}
                  AND completed_time &lt; #{endTimeExclusive}
                  AND order_status IN ('COMPLETED', 'REFUNDED')
                <choose>
                  <when test="storeId != null">
                    AND recharge_store_id = #{storeId}
                  </when>
                  <when test="storeIds != null and storeIds.size() &gt; 0">
                    AND recharge_store_id IN
                    <foreach collection="storeIds" item="scopeStoreId" open="(" separator="," close=")">
                      #{scopeStoreId}
                    </foreach>
                  </when>
                </choose>
                GROUP BY DATE(completed_time)

                UNION ALL

                SELECT DATE(r.completed_time), 0,
                       SUM(r.refund_amount_cent),
                       0, 0, 0
                FROM t_recharge_refund r
                INNER JOIN t_recharge_order o ON o.id = r.recharge_order_id
                WHERE r.completed_time &gt;= #{startTime}
                  AND r.completed_time &lt; #{endTimeExclusive}
                  AND r.refund_status = 'COMPLETED'
                <choose>
                  <when test="storeId != null">
                    AND o.recharge_store_id = #{storeId}
                  </when>
                  <when test="storeIds != null and storeIds.size() &gt; 0">
                    AND o.recharge_store_id IN
                    <foreach collection="storeIds" item="scopeStoreId" open="(" separator="," close=")">
                      #{scopeStoreId}
                    </foreach>
                  </when>
                </choose>
                GROUP BY DATE(r.completed_time)

                UNION ALL

                SELECT DATE(completed_time), 0, 0,
                       SUM(gross_amount_cent),
                       SUM(platform_fee_cent),
                       SUM(store_payable_cent)
                FROM t_consumption_order
                WHERE completed_time &gt;= #{startTime}
                  AND completed_time &lt; #{endTimeExclusive}
                  AND order_status = 'COMPLETED'
                <choose>
                  <when test="storeId != null">
                    AND store_id = #{storeId}
                  </when>
                  <when test="storeIds != null and storeIds.size() &gt; 0">
                    AND store_id IN
                    <foreach collection="storeIds" item="scopeStoreId" open="(" separator="," close=")">
                      #{scopeStoreId}
                    </foreach>
                  </when>
                </choose>
                GROUP BY DATE(completed_time)
            ) daily_metrics
            GROUP BY business_date
            ORDER BY business_date
            </script>
            """)
    List<DashboardDailyAggregate> selectDailyMetrics(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTimeExclusive") LocalDateTime endTimeExclusive,
            @Param("storeId") Long storeId,
            @Param("storeIds") List<Long> storeIds
    );

    @Select("""
            <script>
            SELECT COALESCE(SUM(CASE
                       WHEN order_status = 'PENDING_CONFIRM' AND expires_time &gt; #{now}
                       THEN 1 ELSE 0 END), 0) AS pending_consumption_count,
                   COALESCE(SUM(CASE
                       WHEN order_status = 'COMPLETED' AND settlement_status = 'NOT_INCLUDED'
                       THEN 1 ELSE 0 END), 0) AS not_included_consumption_count,
                   COALESCE(SUM(CASE
                       WHEN order_status = 'COMPLETED' AND settlement_status = 'NOT_INCLUDED'
                       THEN store_payable_cent ELSE 0 END), 0) AS not_included_payable_cent
            FROM t_consumption_order
            WHERE ((order_status = 'PENDING_CONFIRM' AND expires_time &gt; #{now})
               OR (order_status = 'COMPLETED' AND settlement_status = 'NOT_INCLUDED'))
            <choose>
              <when test="storeId != null">
                AND store_id = #{storeId}
              </when>
              <when test="storeIds != null and storeIds.size() &gt; 0">
                AND store_id IN
                <foreach collection="storeIds" item="scopeStoreId" open="(" separator="," close=")">
                  #{scopeStoreId}
                </foreach>
              </when>
            </choose>
            </script>
            """)
    DashboardBacklogAggregate selectConsumptionBacklog(
            @Param("now") LocalDateTime now,
            @Param("storeId") Long storeId,
            @Param("storeIds") List<Long> storeIds
    );

    @Select("""
            <script>
            SELECT COALESCE(SUM(CASE WHEN settlement_status = 'GENERATED' THEN 1 ELSE 0 END), 0)
                       AS awaiting_store_confirmation_count,
                   COALESCE(SUM(CASE WHEN settlement_status = 'GENERATED' THEN payable_amount_cent ELSE 0 END), 0)
                       AS awaiting_store_confirmation_cent,
                   COALESCE(SUM(CASE WHEN settlement_status = 'CONFIRMED' THEN 1 ELSE 0 END), 0)
                       AS awaiting_platform_payment_count,
                   COALESCE(SUM(CASE WHEN settlement_status = 'CONFIRMED' THEN payable_amount_cent ELSE 0 END), 0)
                       AS awaiting_platform_payment_cent
            FROM t_store_settlement
            WHERE settlement_status IN ('GENERATED', 'CONFIRMED')
            <choose>
              <when test="storeId != null">
                AND store_id = #{storeId}
              </when>
              <when test="storeIds != null and storeIds.size() &gt; 0">
                AND store_id IN
                <foreach collection="storeIds" item="scopeStoreId" open="(" separator="," close=")">
                  #{scopeStoreId}
                </foreach>
              </when>
            </choose>
            </script>
            """)
    DashboardBacklogAggregate selectSettlementBacklog(
            @Param("storeId") Long storeId,
            @Param("storeIds") List<Long> storeIds
    );
}
