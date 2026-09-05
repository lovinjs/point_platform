package com.core.coreboot.platform.point.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_point_account")
public class PointAccount {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long customerId;
    private Long availablePoints;
    private Integer version;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
