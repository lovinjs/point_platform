package com.core.coreboot.platform.merchant.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.core.coreboot.platform.common.enums.StoreStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_store")
public class Store {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long merchantId;
    private String storeCode;
    private String storeName;
    private String address;
    private String contactPhone;
    private StoreStatus status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
