package com.core.coreboot.platform.staff.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface StaffAuthorityMapper {

    @Select("""
            SELECT DISTINCT r.role_code
            FROM t_sys_user_role ur
            INNER JOIN t_sys_role r ON r.id = ur.role_id
            WHERE ur.user_id = #{userId}
              AND r.status = 'ACTIVE'
            ORDER BY r.role_code
            """)
    List<String> selectRoleCodes(@Param("userId") Long userId);

    @Select("""
            SELECT store_id
            FROM t_sys_user_store
            WHERE user_id = #{userId}
            ORDER BY store_id
            """)
    List<Long> selectStoreIds(@Param("userId") Long userId);
}
