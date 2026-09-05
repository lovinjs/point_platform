package com.core.coreboot.category.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.core.coreboot.category.dto.UpdateCategoryReq;
import com.core.coreboot.category.entity.Category;
import com.core.coreboot.common.ApiRestResponse;
import com.core.coreboot.category.dto.AddCategoryReq;
import com.core.coreboot.common.dto.BasePageReq;
import com.core.coreboot.category.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;

import jakarta.validation.Valid;

@Tag(name = "分类模块 - 管理员", description = "分类模块")
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/category")
public class CategoryAdminController {

    private final CategoryService categoryService;

    @Operation(summary = "添加分类", description = "添加分类")
    @PostMapping("/add")
    @ResponseBody
    public ApiRestResponse<Object> addCategory(@Valid @RequestBody AddCategoryReq addCategoryReq) {
        categoryService.add(addCategoryReq);
        return ApiRestResponse.success();
    }

    @Operation(summary = "更新分类", description = "更新分类")
    @PostMapping("/update")
    @ResponseBody
    public ApiRestResponse<Object> updateCategory(@Valid @RequestBody UpdateCategoryReq updateCategoryReq) {
        categoryService.update(updateCategoryReq);
        return ApiRestResponse.success();
    }

    @Operation(summary = "删除分类", description = "删除分类")
    @PostMapping("/delete")
    @ResponseBody
    public ApiRestResponse<Object> deleteCategory(@Parameter(description = "分类id") @RequestParam Integer id) {
        categoryService.delete(id);
        return ApiRestResponse.success();
    }

    @Operation(summary = "管理员查询分类列表", description = "管理员查询分类列表")
    @GetMapping("/list")
    @ResponseBody
    public ApiRestResponse<Object> getCategoryListForAdmin(@Valid BasePageReq basePageReq) {
        Page<Category> page = categoryService.getListForAdmin(basePageReq);
        return ApiRestResponse.success(page);
    }
}

