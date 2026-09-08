package com.core.coreboot.platform.settlement.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.core.coreboot.platform.settlement.entity.SettlementPeriod;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDate;
import java.time.LocalDateTime;

public interface SettlementPeriodMapper extends BaseMapper<SettlementPeriod> {

    @Insert("""
            INSERT INTO t_settlement_period
                (period_code, start_date, end_date, period_status)
            VALUES
                (#{periodCode}, #{startDate}, #{endDate}, 'OPEN')
            ON DUPLICATE KEY UPDATE period_code = period_code
            """)
    int insertOpenPeriod(
            @Param("periodCode") String periodCode,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Select("""
            SELECT id, period_code, start_date, end_date, period_status,
                   frozen_time, generated_time, create_time, update_time
            FROM t_settlement_period
            WHERE period_code = #{periodCode}
            LIMIT 1
            """)
    SettlementPeriod selectByPeriodCode(@Param("periodCode") String periodCode);

    @Select("""
            SELECT id, period_code, start_date, end_date, period_status,
                   frozen_time, generated_time, create_time, update_time
            FROM t_settlement_period
            WHERE period_code = #{periodCode}
            LIMIT 1
            FOR UPDATE
            """)
    SettlementPeriod selectByPeriodCodeForUpdate(@Param("periodCode") String periodCode);

    @Update("""
            UPDATE t_settlement_period
            SET period_status = 'GENERATED',
                frozen_time = #{generatedTime},
                generated_time = #{generatedTime},
                update_time = CURRENT_TIMESTAMP
            WHERE id = #{periodId}
              AND period_status = 'OPEN'
            """)
    int markGenerated(
            @Param("periodId") Long periodId,
            @Param("generatedTime") LocalDateTime generatedTime
    );

    @Update("""
            UPDATE t_settlement_period
            SET period_status = 'CONFIRMED',
                update_time = CURRENT_TIMESTAMP
            WHERE id = #{periodId}
              AND period_status = 'GENERATED'
              AND EXISTS (
                    SELECT 1 FROM t_store_settlement s WHERE s.period_id = #{periodId}
              )
              AND NOT EXISTS (
                    SELECT 1
                    FROM t_store_settlement s
                    WHERE s.period_id = #{periodId}
                      AND s.settlement_status = 'GENERATED'
              )
            """)
    int markConfirmedIfAll(@Param("periodId") Long periodId);

    @Update("""
            UPDATE t_settlement_period
            SET period_status = 'PAID',
                update_time = CURRENT_TIMESTAMP
            WHERE id = #{periodId}
              AND period_status IN ('GENERATED', 'CONFIRMED')
              AND EXISTS (
                    SELECT 1 FROM t_store_settlement s WHERE s.period_id = #{periodId}
              )
              AND NOT EXISTS (
                    SELECT 1
                    FROM t_store_settlement s
                    WHERE s.period_id = #{periodId}
                      AND s.settlement_status IN ('GENERATED', 'CONFIRMED')
              )
            """)
    int markPaidIfAll(@Param("periodId") Long periodId);
}
