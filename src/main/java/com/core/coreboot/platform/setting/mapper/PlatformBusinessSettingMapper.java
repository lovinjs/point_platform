package com.core.coreboot.platform.setting.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.core.coreboot.platform.setting.entity.PlatformBusinessSetting;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

public interface PlatformBusinessSettingMapper extends BaseMapper<PlatformBusinessSetting> {

    @Select("""
            SELECT id, platform_fee_rate_bps, consumption_pending_ttl_minutes,
                   version, last_updated_by, create_time, update_time
            FROM t_platform_business_setting
            WHERE id = #{settingId}
            FOR UPDATE
            """)
    PlatformBusinessSetting selectByIdForUpdate(@Param("settingId") Long settingId);

    @Update("""
            UPDATE t_platform_business_setting
            SET platform_fee_rate_bps = #{platformFeeRateBps},
                consumption_pending_ttl_minutes = #{consumptionPendingTtlMinutes},
                version = version + 1,
                last_updated_by = #{operatorId},
                update_time = #{updateTime}
            WHERE id = #{settingId}
              AND version = #{expectedVersion}
            """)
    int updateWithVersion(
            @Param("settingId") Long settingId,
            @Param("platformFeeRateBps") int platformFeeRateBps,
            @Param("consumptionPendingTtlMinutes") int consumptionPendingTtlMinutes,
            @Param("expectedVersion") long expectedVersion,
            @Param("operatorId") Long operatorId,
            @Param("updateTime") LocalDateTime updateTime
    );
}
