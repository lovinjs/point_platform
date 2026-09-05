package com.core.coreboot.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("t_order")
public class Order {
    @TableId(type = IdType.AUTO)
    private Integer id;

    private Integer userId;

    private String orderNo;

    private Integer orderStatus;

    private BigDecimal totalPrice;

    private String receiverName;

    private String receiverPhone;

    private String receiverAddress;

    private BigDecimal postage;

    private Integer paymentType;

    private Date deliveryTime;

    private Date payTime;

    private Date endTime;

    private Integer isDeleted;

    private Date createTime;

    private Date updateTime;
}
