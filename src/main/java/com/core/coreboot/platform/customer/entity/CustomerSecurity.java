package com.core.coreboot.platform.customer.entity;

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
@TableName("t_customer_security")
public class CustomerSecurity {
    @TableId(value = "customer_id", type = IdType.INPUT)
    private Long customerId;
    private String consumePinHash;
    private Integer failedCount;
    private LocalDateTime lockedUntil;
    private LocalDateTime pinUpdatedTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
