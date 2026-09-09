package com.core.coreboot.platform.merchant.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AdminStoreUpdateRequest(
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
    public AdminStoreUpdateCommand toCommand(Long storeId, Long operatorId, String clientIp) {
        return new AdminStoreUpdateCommand(
                storeId,
                storeName,
                address,
                contactPhone,
                operatorId,
                clientIp
        );
    }
}
