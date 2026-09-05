package com.core.coreboot.category.converter;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import com.core.coreboot.category.entity.Category;
import com.core.coreboot.category.dto.AddCategoryReq;
import com.core.coreboot.category.dto.UpdateCategoryReq;
import com.core.coreboot.category.vo.CategoryVO;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface CategoryConverter {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    Category fromAddReq(AddCategoryReq addCategoryReq);

    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    Category fromUpdateReq(UpdateCategoryReq updateCategoryReq);

    @Mapping(target = "childrenList", ignore = true)
    CategoryVO toVO(Category category);

    List<CategoryVO> toVOList(List<Category> categoryList);
}