package com.core.coreboot.platform.merchant.controller;

import com.core.coreboot.common.ApiRestResponse;
import com.core.coreboot.platform.common.model.PageResult;
import com.core.coreboot.platform.merchant.model.CustomerStoreView;
import com.core.coreboot.platform.merchant.service.CustomerStoreQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "积分平台 - 客户合作门店")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/customer/stores")
public class CustomerStoreController {
    private final CustomerStoreQueryService storeQueryService;

    @Operation(summary = "分页查询正常营业的合作门店，无需登录")
    @GetMapping
    public ApiRestResponse<PageResult<CustomerStoreView>> list(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String keyword
    ) {
        return ApiRestResponse.success(storeQueryService.list(pageNum, pageSize, keyword));
    }

    @Operation(summary = "查询正常营业的合作门店详情，无需登录")
    @GetMapping("/{storeId}")
    public ApiRestResponse<CustomerStoreView> detail(@PathVariable Long storeId) {
        return ApiRestResponse.success(storeQueryService.getById(storeId));
    }
}
