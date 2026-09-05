package com.core.coreboot.platform.staff.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.core.coreboot.platform.staff.entity.SysRole;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface SysRoleMapper extends BaseMapper<SysRole> {

    @Select("""
            SELECT id, role_code, role_name, description, status, create_time, update_time
            FROM t_sys_role
            WHERE role_code = #{roleCode}
            LIMIT 1
            """)
    SysRole selectByRoleCode(@Param("roleCode") String roleCode);

    @Select("""
            SELECT COUNT(1)
            FROM t_sys_user_role ur
            INNER JOIN t_sys_role r ON r.id = ur.role_id
            WHERE r.role_code = 'SUPER_ADMIN'
            """)
    long countAssignedSuperAdmins();
}
