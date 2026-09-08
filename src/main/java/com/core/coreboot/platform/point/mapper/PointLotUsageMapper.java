package com.core.coreboot.platform.point.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.core.coreboot.platform.point.entity.PointLotUsage;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface PointLotUsageMapper extends BaseMapper<PointLotUsage> {

    @Select("""
            SELECT COUNT(1)
            FROM t_point_lot_usage
            WHERE point_lot_id = #{pointLotId}
            """)
    long countByPointLotId(@Param("pointLotId") Long pointLotId);
}
