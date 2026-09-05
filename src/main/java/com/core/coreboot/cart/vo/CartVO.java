package com.core.coreboot.cart.vo;

import com.core.coreboot.utils.PriceUtils;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CartVO implements Serializable {
    private Integer id;

    private Integer userId;

    private Integer productId;

    private String productName;

    private String productImage;

    private Integer quantity;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal price;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal totalPrice;

    public void setPrice(BigDecimal price) {
        this.price = PriceUtils.format(price);
    }
}