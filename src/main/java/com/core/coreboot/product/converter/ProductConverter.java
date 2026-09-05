package com.core.coreboot.product.converter;

import com.core.coreboot.product.dto.AddProductReq;
import com.core.coreboot.product.dto.UpdateProductReq;
import com.core.coreboot.product.entity.Product;
import com.core.coreboot.product.vo.ProductVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ProductConverter {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "sale", ignore = true)
    @Mapping(target = "totalSale", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    Product fromAddReq(AddProductReq addProductReq);

    @Mapping(target = "sale", ignore = true)
    @Mapping(target = "totalSale", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    Product fromUpdateReq(UpdateProductReq updateProductReq);

    ProductVO toProductVO(Product product);
}

