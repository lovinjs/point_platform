package com.core.coreboot.platform.settlement.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.core.coreboot.platform.common.enums.SettlementPeriodStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_settlement_period")
public class SettlementPeriod {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String periodCode;
    private LocalDate startDate;
    private LocalDate endDate;
    private SettlementPeriodStatus periodStatus;
    private LocalDateTime frozenTime;
    private LocalDateTime generatedTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
