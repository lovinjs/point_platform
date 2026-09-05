package com.core.coreboot.cart.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Cart {
    @TableId(type = IdType.AUTO)
    private Integer id;

    private Integer userId;

    private Integer productId;

    private Integer quantity;

    private Date createTime;

    private Date updateTime;
}