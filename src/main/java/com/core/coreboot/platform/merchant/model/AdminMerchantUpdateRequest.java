package com.core.coreboot.platform.merchant.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AdminMerchantUpdateRequest(
        @NotBlank(message = "签约主体名称不能为空")
        @Size(max = 200, message = "签约主体名称不能超过200个字符")
        String legalName,

        @NotBlank(message = "商户名称不能为空")
        @Size(max = 200, message = "商户名称不能超过200个字符")
        String businessName,

        @Pattern(regexp = "^$|[0-9A-Za-z]{18}", message = "统一社会信用代码应为18位字母或数字")
        String unifiedSocialCreditCode,

        @NotBlank(message = "联系人不能为空")
        @Size(max = 64, message = "联系人不能超过64个字符")
        String contactName,

        @NotBlank(message = "联系电话不能为空")
        @Size(max = 32, message = "联系电话不能超过32个字符")
        @Pattern(regexp = "[0-9+()\\-\\s]{5,32}", message = "联系电话格式不正确")
        String contactPhone,

        @Size(max = 100, message = "合作协议编号不能超过100个字符")
        String agreementNo
) {
    public AdminMerchantUpdateCommand toCommand(Long merchantId, Long operatorId, String clientIp) {
        return new AdminMerchantUpdateCommand(
                merchantId,
                legalName,
                businessName,
                unifiedSocialCreditCode,
                contactName,
                contactPhone,
                agreementNo,
                operatorId,
                clientIp
        );
    }
}
