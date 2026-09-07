package com.core.coreboot.platform.customer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.core.coreboot.platform.customer.entity.CustomerIdentity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface CustomerIdentityMapper extends BaseMapper<CustomerIdentity> {

    @Select("""
            SELECT id, customer_id, provider_code, app_id, external_id, union_id,
                   create_time, update_time
            FROM t_customer_identity
            WHERE provider_code = #{providerCode}
              AND app_id = #{appId}
              AND external_id = #{externalId}
            LIMIT 1
            """)
    CustomerIdentity selectByExternalIdentity(
            @Param("providerCode") String providerCode,
            @Param("appId") String appId,
            @Param("externalId") String externalId
    );

    @Insert("""
            INSERT IGNORE INTO t_customer_identity
                (customer_id, provider_code, app_id, external_id, union_id)
            VALUES
                (#{customerId}, #{providerCode}, #{appId}, #{externalId}, #{unionId})
            """)
    int insertIgnore(CustomerIdentity identity);
}
