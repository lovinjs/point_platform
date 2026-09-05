package com.core.coreboot.platform.recharge.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.core.coreboot.platform.common.enums.FundReceiver;
import com.core.coreboot.platform.common.enums.PaymentMethod;
import com.core.coreboot.platform.common.enums.RechargeChannel;
import com.core.coreboot.platform.common.enums.RechargeOrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_recharge_order")
public class RechargeOrder {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String orderNo;
    private Long customerId;
    private Long rechargeStoreId;
    private Long rechargePoints;
    private Long amountCent;
    private RechargeChannel channel;
    private PaymentMethod paymentMethod;
    private FundReceiver fundReceiver;
    private String paymentReference;
    private RechargeOrderStatus orderStatus;
    private Long operatorId;
    private LocalDateTime paidTime;
    private LocalDateTime completedTime;
    private String idempotencyKey;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
