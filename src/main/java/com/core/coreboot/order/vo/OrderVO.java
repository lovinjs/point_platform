package com.core.coreboot.order.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderVO {

    private Integer userId;

    private String orderNo;

    private Integer orderStatus;

    private String orderStatusName;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal totalPrice;

    private String receiverName;

    private String receiverPhone;

    private String receiverAddress;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal postage;

    private Integer paymentType;

    private Date deliveryTime;

    private Date payTime;

    private Date endTime;

    private Date createTime;

    private Date updateTime;

    private List<OrderItemVO> orderItemVOList;
}