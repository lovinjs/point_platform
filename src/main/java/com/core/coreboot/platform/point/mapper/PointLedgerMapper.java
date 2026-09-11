package com.core.coreboot.platform.point.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.core.coreboot.platform.point.entity.PointLedger;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface PointLedgerMapper extends BaseMapper<PointLedger> {

    @Select("""
            SELECT id, ledger_no, account_id, customer_id, delta_points, balance_after,
                   ledger_type, business_type, business_no, operator_id, store_id,
                   idempotency_key, remark, create_time
            FROM t_point_ledger
            WHERE idempotency_key = #{idempotencyKey}
            LIMIT 1
            """)
    PointLedger selectByIdempotencyKey(@Param("idempotencyKey") String idempotencyKey);
}
