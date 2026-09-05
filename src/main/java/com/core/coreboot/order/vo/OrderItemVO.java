package com.core.coreboot.order.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderItemVO {

    private String orderNo;

    private Integer productId;

    private String productName;

    private String productImage;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal unitPrice;

    private Integer quantity;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal totalPrice;
}