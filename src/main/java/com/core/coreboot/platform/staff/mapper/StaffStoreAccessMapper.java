package com.core.coreboot.platform.staff.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface StaffStoreAccessMapper {

    @Select("""
            SELECT r.role_code
            FROM t_sys_user_role ur
            INNER JOIN t_sys_role r ON r.id = ur.role_id AND r.status = 'ACTIVE'
            LEFT JOIN t_sys_user_store us
                ON us.user_id = ur.user_id AND us.store_id = #{storeId}
            WHERE ur.user_id = #{userId}
              AND (
                    r.role_code = 'SUPER_ADMIN'
                    OR (r.role_code IN ('CLERK', 'STORE_MANAGER') AND us.id IS NOT NULL)
                  )
            ORDER BY CASE r.role_code
                         WHEN 'SUPER_ADMIN' THEN 1
                         WHEN 'STORE_MANAGER' THEN 2
                         WHEN 'CLERK' THEN 3
                         ELSE 4
                     END
            LIMIT 1
            """)
    String findEffectiveRoleCode(@Param("userId") Long userId, @Param("storeId") Long storeId);
}
