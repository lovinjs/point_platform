package com.core.coreboot.platform.point.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.core.coreboot.platform.point.entity.PointLot;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface PointLotMapper extends BaseMapper<PointLot> {

    @Select("""
            SELECT id, customer_id, source_recharge_order_id, total_points, remaining_points,
                   lot_status, create_time, update_time
            FROM t_point_lot
            WHERE customer_id = #{customerId}
              AND lot_status = 'AVAILABLE'
              AND remaining_points > 0
            ORDER BY create_time, id
            FOR UPDATE
            """)
    List<PointLot> selectAvailableByCustomerIdForUpdate(@Param("customerId") Long customerId);

    @Update("""
            UPDATE t_point_lot
            SET remaining_points = remaining_points - #{usedPoints},
                lot_status = CASE
                    WHEN remaining_points = #{usedPoints} THEN 'DEPLETED'
                    ELSE 'AVAILABLE'
                END,
                update_time = CURRENT_TIMESTAMP
            WHERE id = #{lotId}
              AND customer_id = #{customerId}
              AND lot_status = 'AVAILABLE'
              AND remaining_points >= #{usedPoints}
            """)
    int consumePoints(
            @Param("lotId") Long lotId,
            @Param("customerId") Long customerId,
            @Param("usedPoints") Long usedPoints
    );

    @Select("""
            SELECT id, customer_id, source_recharge_order_id, total_points, remaining_points,
                   lot_status, create_time, update_time
            FROM t_point_lot
            WHERE source_recharge_order_id = #{rechargeOrderId}
            LIMIT 1
            FOR UPDATE
            """)
    PointLot selectByRechargeOrderIdForUpdate(@Param("rechargeOrderId") Long rechargeOrderId);

    @Update("""
            UPDATE t_point_lot
            SET lot_status = 'REFUNDED',
                update_time = CURRENT_TIMESTAMP
            WHERE id = #{lotId}
              AND customer_id = #{customerId}
              AND lot_status = 'AVAILABLE'
              AND total_points = remaining_points
            """)
    int markUntouchedLotRefunded(
            @Param("lotId") Long lotId,
            @Param("customerId") Long customerId
    );

    @Select("""
            SELECT id, customer_id, source_recharge_order_id, total_points, remaining_points,
                   lot_status, create_time, update_time
            FROM t_point_lot
            WHERE id = #{lotId}
            FOR UPDATE
            """)
    PointLot selectByIdForUpdate(@Param("lotId") Long lotId);

    @Update("""
            UPDATE t_point_lot
            SET remaining_points = remaining_points + #{restoredPoints},
                lot_status = 'AVAILABLE',
                update_time = CURRENT_TIMESTAMP
            WHERE id = #{lotId}
              AND customer_id = #{customerId}
              AND lot_status IN ('AVAILABLE', 'DEPLETED')
              AND remaining_points <= total_points - #{restoredPoints}
            """)
    int restorePoints(
            @Param("lotId") Long lotId,
            @Param("customerId") Long customerId,
            @Param("restoredPoints") Long restoredPoints
    );
}
