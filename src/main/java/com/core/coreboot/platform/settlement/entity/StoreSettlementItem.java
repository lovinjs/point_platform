package com.core.coreboot.platform.settlement.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.core.coreboot.platform.common.enums.SettlementItemType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_store_settlement_item")
public class StoreSettlementItem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long settlementId;
    private SettlementItemType itemType;
    private Long consumptionOrderId;
    private Long pointsDelta;
    private Long grossAmountCent;
    private Long platformFeeCent;
    private Long storePayableCent;
    private String adjustmentReason;
    private LocalDateTime createTime;
}
