package com.core.coreboot.platform.recharge.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.core.coreboot.platform.common.enums.RechargeRefundStatus;
import com.core.coreboot.platform.common.enums.RefundMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_recharge_refund")
public class RechargeRefund {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String refundNo;
    private Long rechargeOrderId;
    private Long customerId;
    private Long refundPoints;
    private Long refundAmountCent;
    private RefundMethod refundMethod;
    private String refundReference;
    private RechargeRefundStatus refundStatus;
    private Long operatorId;
    private String reason;
    private LocalDateTime completedTime;
    private String idempotencyKey;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
