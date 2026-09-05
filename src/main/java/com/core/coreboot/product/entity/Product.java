package com.core.coreboot.product.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Product {
    @TableId(type = IdType.AUTO)
    private Integer id;

    private String name;

    private String image;

    private String detail;

    private Integer categoryId;

    private BigDecimal price;

    private Integer stock;

    private Integer sale;

    private Integer virtualSale;

    @TableField(value = "total_sale", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private Integer totalSale;

    private Integer status;

    private Integer isDeleted;

    private Date createTime;

    private Date updateTime;
}