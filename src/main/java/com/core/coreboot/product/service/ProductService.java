package com.core.coreboot.product.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.core.coreboot.product.entity.Product;
import com.core.coreboot.product.dto.AddProductReq;
import com.core.coreboot.product.dto.ProductListReq;
import com.core.coreboot.product.dto.UpdateProductReq;
import com.core.coreboot.product.vo.ProductVO;

import java.util.List;

public interface ProductService extends IService<Product> {

    void add(AddProductReq addProductReq);

    void update(UpdateProductReq updateProductReq);

    void delete(Integer id);

    Product getProductDetailForAdmin(Integer id);

    ProductVO getProductDetailForUser(Integer id);

    List<Product> getProductListByIds(List<Integer> ids);

    IPage<Product> getListForAdmin(ProductListReq productListReq);

    IPage<ProductVO> getListForUser(ProductListReq productListReq);

    void batchUpdateStatus(Integer[] ids, Integer status);

    void increaseSale(Integer productId, Integer quantity);

    void decreaseStock(Integer productId, Integer quantity);

    void increaseStock(Integer productId, Integer quantity);

    void validProductStatusAndStock(Product product, Integer count);
}
