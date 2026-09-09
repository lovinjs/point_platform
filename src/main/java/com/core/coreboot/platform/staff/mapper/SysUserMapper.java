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

    @Select("""
            SELECT id, username, phone, password_hash, real_name, status,
                   failed_login_count, locked_until, token_version, password_updated_time,
                   last_login_time, create_time, update_time
            FROM t_sys_user
            WHERE id = #{userId}
            FOR UPDATE
            """)
    SysUser selectByIdForUpdate(@Param("userId") Long userId);

    @Update("""
            UPDATE t_sys_user
            SET phone = #{phone},
                real_name = #{realName},
                token_version = token_version + #{tokenVersionIncrement},
                update_time = #{updateTime}
            WHERE id = #{userId}
            """)
    int updateStaffProfile(
            @Param("userId") Long userId,
            @Param("phone") String phone,
            @Param("realName") String realName,
            @Param("tokenVersionIncrement") int tokenVersionIncrement,
            @Param("updateTime") LocalDateTime updateTime
    );

    @Update("""
            UPDATE t_sys_user
            SET status = #{status},
                failed_login_count = 0,
                locked_until = NULL,
                token_version = token_version + 1,
                update_time = #{updateTime}
            WHERE id = #{userId}
            """)
    int updateStaffStatus(
            @Param("userId") Long userId,
            @Param("status") String status,
            @Param("updateTime") LocalDateTime updateTime
    );

    @Update("""
            UPDATE t_sys_user
            SET password_hash = #{passwordHash},
                failed_login_count = 0,
                locked_until = NULL,
                token_version = token_version + 1,
                password_updated_time = #{passwordUpdatedTime},
                update_time = #{passwordUpdatedTime}
            WHERE id = #{userId}
            """)
    int resetStaffPassword(
            @Param("userId") Long userId,
            @Param("passwordHash") String passwordHash,
            @Param("passwordUpdatedTime") LocalDateTime passwordUpdatedTime
    );

    @Update("""
            UPDATE t_sys_user
            SET status = CASE WHEN status = 'LOCKED' THEN 'ACTIVE' ELSE status END,
                failed_login_count = 0,
                locked_until = NULL,
                token_version = token_version + 1,
                update_time = #{updateTime}
            WHERE id = #{userId}
            """)
    int unlockStaffLogin(
            @Param("userId") Long userId,
            @Param("updateTime") LocalDateTime updateTime
    );

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
