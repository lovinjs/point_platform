package com.core.coreboot.platform.merchant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.core.coreboot.platform.merchant.entity.Store;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface StoreMapper extends BaseMapper<Store> {

    @Select("""
            SELECT DISTINCT s.id, s.merchant_id, s.store_code, s.store_name, s.address,
                            s.contact_phone, s.status, s.create_time, s.update_time
            FROM t_store s
            INNER JOIN t_merchant m ON m.id = s.merchant_id AND m.status = 'ACTIVE'
            WHERE s.status = 'ACTIVE'
              AND (
                    EXISTS (
                        SELECT 1
                        FROM t_sys_user_role ur
                        INNER JOIN t_sys_role r ON r.id = ur.role_id AND r.status = 'ACTIVE'
                        WHERE ur.user_id = #{userId}
                          AND r.role_code = 'SUPER_ADMIN'
                    )
                    OR EXISTS (
                        SELECT 1
                        FROM t_sys_user_store us
                        WHERE us.user_id = #{userId}
                          AND us.store_id = s.id
                    )
                  )
            ORDER BY s.store_name, s.id
            """)
    List<Store> selectAccessibleActiveStores(@Param("userId") Long userId);

    @Select("""
            SELECT id, merchant_id, store_code, store_name, address, contact_phone,
                   status, create_time, update_time
            FROM t_store
            WHERE id = #{storeId}
            FOR UPDATE
            """)
    Store selectByIdForUpdate(@Param("storeId") Long storeId);
}
