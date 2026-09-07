package com.core.coreboot.platform.customer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.core.coreboot.platform.customer.entity.CustomerSecurity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface CustomerSecurityMapper extends BaseMapper<CustomerSecurity> {

    @Select("""
            SELECT customer_id, consume_pin_hash, failed_count, locked_until,
                   pin_updated_time, create_time, update_time
            FROM t_customer_security
            WHERE customer_id = #{customerId}
            FOR UPDATE
            """)
    CustomerSecurity selectByCustomerIdForUpdate(@Param("customerId") Long customerId);
}
