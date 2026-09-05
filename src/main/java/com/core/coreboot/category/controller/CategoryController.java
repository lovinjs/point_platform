package com.core.coreboot.category.controller;

import com.core.coreboot.common.ApiRestResponse;
import com.core.coreboot.category.vo.CategoryVO;
import com.core.coreboot.category.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Tag(name = "分类模块 - 用户", description = "分类模块")
@Controller
@RequiredArgsConstructor
@RequestMapping("/category")
public class CategoryController {

    private final CategoryService categoryService;

    @Operation(summary = "用户查询分类列表", description = "用户查询分类列表")
    @PostMapping("/list")
    @ResponseBody
    public ApiRestResponse<Object> getCategoryListForUser() {
        List<CategoryVO> categoryVOList = categoryService.getListForUser();
        return ApiRestResponse.success(categoryVOList);
    }
}
