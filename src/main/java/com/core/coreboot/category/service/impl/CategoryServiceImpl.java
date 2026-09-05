package com.core.coreboot.category.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.core.coreboot.category.dto.UpdateCategoryReq;
import com.core.coreboot.common.dto.BasePageReq;
import com.core.coreboot.exception.CustomException;
import com.core.coreboot.exception.ExceptionEnum;
import com.core.coreboot.category.mapper.CategoryMapper;
import com.core.coreboot.category.entity.Category;
import com.core.coreboot.category.dto.AddCategoryReq;
import com.core.coreboot.category.vo.CategoryVO;
import com.core.coreboot.category.service.CategoryService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.core.coreboot.category.converter.CategoryConverter;
import com.core.coreboot.utils.TreeUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {

    private final CategoryConverter categoryConverter;

    @Override
    @CacheEvict(value = "getListForUser", allEntries = true)
    public void add(AddCategoryReq addCategoryReq) {
        Category category = categoryConverter.fromAddReq(addCategoryReq);
        long count = count(new LambdaQueryWrapper<Category>().eq(Category::getName, category.getName()));
        if (count > 0) {
            throw new CustomException(ExceptionEnum.NAME_EXISTED);
        }
        try {
            save(category);
        } catch (Exception e) {
            throw new CustomException(ExceptionEnum.INSERT_FAILED);
        }
    }

    @Override
    @CacheEvict(value = "getListForUser", allEntries = true)
    public void delete(Integer id) {
        try {
            boolean success = removeById(id);
            if (!success) {
                throw new CustomException(ExceptionEnum.DELETE_FAILED);
            }
        } catch (Exception e) {
            throw new CustomException(ExceptionEnum.DELETE_FAILED);
        }
    }

    @Override
    @CacheEvict(value = "getListForUser", allEntries = true)
    public void update(UpdateCategoryReq updateCategoryReq) {
        Category category = categoryConverter.fromUpdateReq(updateCategoryReq);
        Category dbCategory = getOne(new LambdaQueryWrapper<Category>()
                .select(Category::getId)
                .eq(Category::getName, category.getName())
        );
        if (dbCategory != null && !dbCategory.getId().equals(category.getId())) {
            throw new CustomException(ExceptionEnum.NAME_EXISTED);
        }
        boolean success = updateById(category);
        if (!success) {
            throw new CustomException(ExceptionEnum.UPDATE_FAILED);
        }
    }

    @Override
    public Page<Category> getListForAdmin(BasePageReq basePageReq) {
        Page<Category> page = basePageReq.getPaginationPage();
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(Category::getRank, Category::getOrder);
        return page(page, wrapper);
    }

    @Override
    @Cacheable(value = "getListForUser")
    public List<CategoryVO> getListForUser() {
        List<Category> categoryList = list();
        List<CategoryVO> categoryVOList = categoryConverter.toVOList(categoryList);
        return TreeUtils.buildTree(categoryVOList, 0);
    }
}
