package com.core.coreboot.platform.merchant.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record AdminStoreCreateRequest(
        @NotNull(message = "所属商户不能为空")
        @Positive(message = "所属商户不正确")
        Long merchantId,

        @NotBlank(message = "门店编码不能为空")
        @Size(max = 64, message = "门店编码不能超过64个字符")
        @Pattern(regexp = "[A-Za-z0-9_-]+", message = "门店编码只能包含字母、数字、横线和下划线")
        String storeCode,

        @NotBlank(message = "门店名称不能为空")
        @Size(max = 200, message = "门店名称不能超过200个字符")
        String storeName,

        @NotBlank(message = "门店地址不能为空")
        @Size(max = 500, message = "门店地址不能超过500个字符")
        String address,

        @NotBlank(message = "联系电话不能为空")
        @Size(max = 32, message = "联系电话不能超过32个字符")
        @Pattern(regexp = "[0-9+()\\-\\s]{5,32}", message = "联系电话格式不正确")
        String contactPhone
) {
    public AdminStoreCreateCommand toCommand(Long operatorId, String clientIp) {
        return new AdminStoreCreateCommand(
                merchantId,
                storeCode,
                storeName,
                address,
                contactPhone,
                operatorId,
                clientIp
        );
    }
}
