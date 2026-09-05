package com.core.coreboot.platform.staff.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.core.coreboot.platform.staff.entity.SysUser;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

public interface SysUserMapper extends BaseMapper<SysUser> {

    @Select("""
            SELECT id, username, phone, password_hash, real_name, status,
                   failed_login_count, locked_until, token_version, password_updated_time,
                   last_login_time, create_time, update_time
            FROM t_sys_user
            WHERE username = #{username}
            LIMIT 1
            """)
    SysUser selectByUsername(@Param("username") String username);

    @Update("""
            UPDATE t_sys_user
            SET failed_login_count = failed_login_count + 1,
                locked_until = CASE
                    WHEN failed_login_count + 1 >= #{maxFailedAttempts} THEN #{lockedUntil}
                    ELSE locked_until
                END,
                update_time = CURRENT_TIMESTAMP
            WHERE username = #{username}
              AND status = 'ACTIVE'
              AND (locked_until IS NULL OR locked_until <= CURRENT_TIMESTAMP)
            """)
    int recordLoginFailure(
            @Param("username") String username,
            @Param("maxFailedAttempts") int maxFailedAttempts,
            @Param("lockedUntil") LocalDateTime lockedUntil
    );

    @Update("""
            UPDATE t_sys_user
            SET failed_login_count = 0,
                locked_until = NULL,
                last_login_time = #{loginTime},
                update_time = CURRENT_TIMESTAMP
            WHERE id = #{userId}
              AND status = 'ACTIVE'
            """)
    int recordLoginSuccess(@Param("userId") Long userId, @Param("loginTime") LocalDateTime loginTime);
}
