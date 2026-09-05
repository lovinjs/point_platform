package com.core.coreboot.platform.point.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.core.coreboot.platform.common.enums.PointLedgerBusinessType;
import com.core.coreboot.platform.common.enums.PointLedgerType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_point_ledger")
public class PointLedger {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String ledgerNo;
    private Long accountId;
    private Long customerId;
    private Long deltaPoints;
    private Long balanceAfter;
    private PointLedgerType ledgerType;
    private PointLedgerBusinessType businessType;
    private String businessNo;
    private Long operatorId;
    private Long storeId;
    private String idempotencyKey;
    private String remark;
    private LocalDateTime createTime;
}
