package com.core.coreboot.product.vo;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.core.coreboot.utils.PriceUtils;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class ProductVO {
    @TableId(type = IdType.AUTO)
    private Integer id;

    private String name;

    private String image;

    private String detail;

    private Integer categoryId;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal price;

    private Integer stock;

    @TableField(value = "total_sale", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private Integer totalSale;

    private Date createTime;

    public void setPrice(BigDecimal price) {
        this.price = PriceUtils.format(price);
    }
}
