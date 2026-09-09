package com.core.coreboot.platform.merchant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.core.coreboot.platform.merchant.entity.Merchant;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface MerchantMapper extends BaseMapper<Merchant> {

    @Select("""
            SELECT id, merchant_code, legal_name, business_name, unified_social_credit_code,
                   contact_name, contact_phone, agreement_no, status, create_time, update_time
            FROM t_merchant
            WHERE id = #{merchantId}
            FOR UPDATE
            """)
    Merchant selectByIdForUpdate(@Param("merchantId") Long merchantId);
}
