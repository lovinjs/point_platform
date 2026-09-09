package com.core.coreboot.platform.merchant.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.core.coreboot.platform.common.enums.MerchantStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_merchant")
public class Merchant {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String merchantCode;
    private String legalName;
    private String businessName;
    private String unifiedSocialCreditCode;
    private String contactName;
    private String contactPhone;
    private String agreementNo;
    private MerchantStatus status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
