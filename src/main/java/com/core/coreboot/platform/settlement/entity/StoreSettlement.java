package com.core.coreboot.platform.settlement.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.core.coreboot.platform.common.enums.StoreSettlementStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_store_settlement")
public class StoreSettlement {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String settlementNo;
    private Long periodId;
    private Long merchantId;
    private Long storeId;
    private Long totalConsumePoints;
    private Long grossAmountCent;
    private Long platformFeeCent;
    private Long adjustmentAmountCent;
    private Long payableAmountCent;
    private StoreSettlementStatus settlementStatus;
    private LocalDateTime confirmedTime;
    private LocalDateTime paidTime;
    private String paymentReference;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
