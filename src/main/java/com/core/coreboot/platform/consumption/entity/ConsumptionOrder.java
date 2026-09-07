package com.core.coreboot.platform.consumption.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.core.coreboot.platform.common.enums.ConsumptionOrderStatus;
import com.core.coreboot.platform.common.enums.ConsumptionVerificationMode;
import com.core.coreboot.platform.common.enums.SettlementStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_consumption_order")
public class ConsumptionOrder {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String orderNo;
    private Long customerId;
    private Long storeId;
    private Long consumePoints;
    private Long grossAmountCent;
    private Integer platformFeeRateBps;
    private Long platformFeeCent;
    private Long storePayableCent;
    private ConsumptionVerificationMode verificationMode;
    private LocalDateTime expiresTime;
    private ConsumptionOrderStatus orderStatus;
    private Long operatorId;
    private LocalDateTime confirmedTime;
    private LocalDateTime completedTime;
    private Long reversedBy;
    private LocalDateTime reversedTime;
    private String reversalReason;
    private SettlementStatus settlementStatus;
    private String idempotencyKey;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
