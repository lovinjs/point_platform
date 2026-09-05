package com.core.coreboot.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotNull;

@Schema(description = "创建订单参数")
public class CreateOrderReq {

    @Schema(description = "收货人姓名")
    @NotNull
    private String receiverName;

    @Schema(description = "收货人手机号")
    @NotNull
    private String receiverPhone;

    @Schema(description = "收货人地址")
    @NotNull
    private String receiverAddress;

    @Schema(description = "邮费")
    private Integer postage = 0;

    @Schema(description = "支付方式")
    private Integer paymentType = 1;

    public String getReceiverName() {
        return receiverName;
    }

    public void setReceiverName(String receiverName) {
        this.receiverName = receiverName;
    }

    public String getReceiverPhone() {
        return receiverPhone;
    }

    public void setReceiverPhone(String receiverPhone) {
        this.receiverPhone = receiverPhone;
    }

    public String getReceiverAddress() {
        return receiverAddress;
    }

    public void setReceiverAddress(String receiverAddress) {
        this.receiverAddress = receiverAddress;
    }

    public Integer getPostage() {
        return postage;
    }

    public void setPostage(Integer postage) {
        this.postage = postage;
    }

    public Integer getPaymentType() {
        return paymentType;
    }

    public void setPaymentType(Integer paymentType) {
        this.paymentType = paymentType;
    }
}