package com.core.coreboot.platform.customer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.core.coreboot.platform.customer.entity.CustomerUser;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface CustomerUserMapper extends BaseMapper<CustomerUser> {

    @Select("""
            SELECT id, phone, nickname, avatar_url, status, last_login_time, token_version,
                   create_time, update_time
            FROM t_customer_user
            WHERE phone = #{phone}
            LIMIT 1
            """)
    CustomerUser selectByPhone(@Param("phone") String phone);

    @Select("""
            SELECT id, phone, nickname, avatar_url, status, last_login_time, token_version,
                   create_time, update_time
            FROM t_customer_user
            WHERE id = #{customerId}
            FOR UPDATE
            """)
    CustomerUser selectByIdForUpdate(@Param("customerId") Long customerId);

    @Select("""
            SELECT id, phone, nickname, avatar_url, status, last_login_time, token_version,
                   create_time, update_time
            FROM t_customer_user
            WHERE phone = #{phone}
            LIMIT 1
            FOR UPDATE
            """)
    CustomerUser selectByPhoneForUpdate(@Param("phone") String phone);

    @Update("""
            UPDATE t_customer_user
            SET phone = #{phone},
                update_time = CURRENT_TIMESTAMP
            WHERE id = #{customerId}
              AND (phone IS NULL OR phone = '')
            """)
    int bindPhoneIfUnbound(
            @Param("customerId") Long customerId,
            @Param("phone") String phone
    );

    @Update("""
            UPDATE t_customer_user
            SET last_login_time = CURRENT_TIMESTAMP,
                update_time = CURRENT_TIMESTAMP
            WHERE id = #{customerId}
            """)
    int updateLastLoginTime(@Param("customerId") Long customerId);

    @Update("""
            UPDATE t_customer_user
            SET nickname = #{nickname},
                avatar_url = #{avatarUrl},
                update_time = CURRENT_TIMESTAMP
            WHERE id = #{customerId}
            """)
    int updateWechatProfile(
            @Param("customerId") Long customerId,
            @Param("nickname") String nickname,
            @Param("avatarUrl") String avatarUrl
    );

    @Update("""
            UPDATE t_customer_user
            SET status = #{status},
                token_version = token_version + 1,
                update_time = #{updateTime}
            WHERE id = #{customerId}
            """)
    int updateCustomerStatus(
            @Param("customerId") Long customerId,
            @Param("status") String status,
            @Param("updateTime") java.time.LocalDateTime updateTime
    );
}
