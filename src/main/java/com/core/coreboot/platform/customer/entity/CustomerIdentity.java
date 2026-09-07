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
@TableName("t_customer_identity")
public class CustomerIdentity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long customerId;
    private String providerCode;
    private String appId;
    private String externalId;
    private String unionId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
