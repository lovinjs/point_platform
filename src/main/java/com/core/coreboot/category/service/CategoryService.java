package com.core.coreboot.category.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.core.coreboot.category.dto.UpdateCategoryReq;
import com.core.coreboot.category.entity.Category;
import com.core.coreboot.category.dto.AddCategoryReq;
import com.core.coreboot.category.vo.CategoryVO;
import com.baomidou.mybatisplus.extension.service.IService;
import com.core.coreboot.common.dto.BasePageReq;

import java.util.List;

public interface CategoryService extends IService<Category> {
    void add(AddCategoryReq addCategoryReq);

    void delete(Integer id);

    void update(UpdateCategoryReq updateCategoryReq);

    Page<Category> getListForAdmin(BasePageReq basePageReq);

    List<CategoryVO> getListForUser();
}
