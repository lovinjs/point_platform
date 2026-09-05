package com.core.coreboot.platform.point.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.core.coreboot.platform.point.entity.PointAccount;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface PointAccountMapper extends BaseMapper<PointAccount> {

    @Insert("""
            INSERT IGNORE INTO t_point_account (customer_id, available_points, version)
            VALUES (#{customerId}, 0, 0)
            """)
    int ensureAccount(@Param("customerId") Long customerId);

    @Select("""
            SELECT id, customer_id, available_points, version, create_time, update_time
            FROM t_point_account
            WHERE customer_id = #{customerId}
            FOR UPDATE
            """)
    PointAccount selectByCustomerIdForUpdate(@Param("customerId") Long customerId);

    @Update("""
            UPDATE t_point_account
            SET available_points = available_points + #{points},
                version = version + 1,
                update_time = CURRENT_TIMESTAMP
            WHERE id = #{accountId}
            """)
    int increaseBalance(@Param("accountId") Long accountId, @Param("points") Long points);
}
