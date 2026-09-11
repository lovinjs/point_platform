package com.core.coreboot.platform.point.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.core.coreboot.platform.point.entity.PointLotUsage;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface PointLotUsageMapper extends BaseMapper<PointLotUsage> {

    @Select("""
            SELECT COUNT(1)
            FROM t_point_lot_usage
            WHERE point_lot_id = #{pointLotId}
            """)
    long countByPointLotId(@Param("pointLotId") Long pointLotId);

    @Select("""
            SELECT id, consumption_order_id, point_lot_id, used_points, reversed_points,
                   create_time, update_time
            FROM t_point_lot_usage
            WHERE consumption_order_id = #{consumptionOrderId}
            ORDER BY id
            FOR UPDATE
            """)
    List<PointLotUsage> selectByConsumptionOrderIdForUpdate(
            @Param("consumptionOrderId") Long consumptionOrderId
    );

    @Update("""
            UPDATE t_point_lot_usage
            SET reversed_points = used_points,
                update_time = CURRENT_TIMESTAMP
            WHERE id = #{usageId}
              AND consumption_order_id = #{consumptionOrderId}
              AND used_points = #{usedPoints}
              AND reversed_points = 0
            """)
    int markFullyReversed(
            @Param("usageId") Long usageId,
            @Param("consumptionOrderId") Long consumptionOrderId,
            @Param("usedPoints") Long usedPoints
    );
}
