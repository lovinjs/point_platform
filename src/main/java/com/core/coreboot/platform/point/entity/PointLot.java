package com.core.coreboot.platform.point.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.core.coreboot.platform.common.enums.PointLotStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_point_lot")
public class PointLot {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long customerId;
    private Long sourceRechargeOrderId;
    private Long totalPoints;
    private Long remainingPoints;
    private PointLotStatus lotStatus;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
