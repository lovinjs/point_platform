package com.core.coreboot.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.core.coreboot.product.constant.ProductSortEnum;
import com.core.coreboot.product.constant.ProductStatus;
import com.core.coreboot.product.converter.ProductConverter;
import com.core.coreboot.product.vo.ProductVO;
import com.core.coreboot.utils.TreeUtils;
import com.core.coreboot.exception.CustomException;
import com.core.coreboot.exception.ExceptionEnum;
import com.core.coreboot.product.mapper.ProductMapper;
import com.core.coreboot.product.entity.Product;
import com.core.coreboot.product.dto.AddProductReq;
import com.core.coreboot.product.dto.ProductListReq;
import com.core.coreboot.product.dto.UpdateProductReq;
import com.core.coreboot.category.vo.CategoryVO;
import com.core.coreboot.category.service.CategoryService;
import com.core.coreboot.product.service.ProductService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements ProductService {

    private final CategoryService categoryService;

    private final ProductConverter productConverter;

    @Override
    public void add(AddProductReq addProductReq) {
        Product product = productConverter.fromAddReq(addProductReq);
        if (categoryService.getById(product.getCategoryId()) == null) {
            throw new CustomException(ExceptionEnum.NOT_EXIST);
        }
        long count = count(new LambdaQueryWrapper<Product>().eq(Product::getName, product.getName()));
        if (count > 0) {
            throw new CustomException(ExceptionEnum.NAME_EXISTED);
        }
        boolean success = save(product);
        if (!success) {
            throw new CustomException(ExceptionEnum.INSERT_FAILED);
        }
    }

    @Override
    public void update(UpdateProductReq updateProductReq) {
        Product product = productConverter.fromUpdateReq(updateProductReq);
        if (categoryService.getById(product.getCategoryId()) == null) {
            throw new CustomException(ExceptionEnum.NOT_EXIST);
        }
        Product dbProduct = getOne(new LambdaQueryWrapper<Product>()
                .select(Product::getId)
                .eq(Product::getName, product.getName())
        );
        // 必须是自己才允许重名
        if (dbProduct != null && !dbProduct.getId().equals(product.getId())) {
            throw new CustomException(ExceptionEnum.NAME_EXISTED);
        }
        boolean success = updateById(product);
        if (!success) {
            throw new CustomException(ExceptionEnum.UPDATE_FAILED);
        }
    }

    @Override
    public void delete(Integer id) {
        boolean success = removeById(id);
        if (!success) {
            throw new CustomException(ExceptionEnum.DELETE_FAILED);
        }
    }

    @Override
    public ProductVO getProductDetailForUser(Integer id) {
        Product product = getById(id);
        if (product == null) {
            throw new CustomException(ExceptionEnum.NOT_EXIST);
        }
        return productConverter.toProductVO(product);
    }

    @Override
    public Product getProductDetailForAdmin(Integer id) {
        Product product = getById(id);
        if (product == null) {
            throw new CustomException(ExceptionEnum.NOT_EXIST);
        }
        return product;
    }

    @Override
    public List<Product> getProductListByIds(List<Integer> ids) {
        List<Product> productList = listByIds(ids);
        if (CollectionUtils.isEmpty(productList)) {
            throw new CustomException(ExceptionEnum.NOT_EXIST);
        }
        return productList;
    }

    @Override
    public IPage<Product> getListForAdmin(ProductListReq productListReq) {
        Page<Product> page = productListReq.getPaginationPage();
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        // 关键字搜索查询
        wrapper.like(!StringUtils.isBlank(productListReq.getKeyword()), Product::getName, productListReq.getKeyword());
        // 排序查询
        ProductSortEnum.getByCode(productListReq.getSortCode()).handle(wrapper);
        return page(page, wrapper);
    }

    @Override
    public IPage<ProductVO> getListForUser(ProductListReq productListReq) {
        Page<Product> page = productListReq.getPaginationPage();
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        // 关键字搜索查询
        wrapper.like(!StringUtils.isBlank(productListReq.getKeyword()), Product::getName, productListReq.getKeyword());
        // 分类查询，查出某分类下所有的产品
        if (productListReq.getCategoryId() != null) {
            // 获取完整分类 id 树
            List<CategoryVO> categoryVOList = categoryService.getListForUser();
            // 查找子分类 id 树
            CategoryVO categoryVONode = TreeUtils.findNodeInTree(categoryVOList, productListReq.getCategoryId());
            if (categoryVONode == null) {
                throw new CustomException(ExceptionEnum.NOT_EXIST);
            }
            // 提取子分类树 id 列表
            ArrayList<Integer> categoryIdList = new ArrayList<>();
            categoryIdList.add(productListReq.getCategoryId());
            collectIds(categoryVONode.getChildrenList(), categoryIdList);
            wrapper.in(Product::getCategoryId, categoryIdList);
        }
        // 排序查询
        ProductSortEnum.getByCode(productListReq.getSortCode()).handle(wrapper);
        // 只查上架产品
        wrapper.eq(Product::getStatus, ProductStatus.SALE);
        return page(page, wrapper).convert(productConverter::toProductVO);
    }

    private void collectIds(List<CategoryVO> categoryVOList, ArrayList<Integer> categoryIdList) {
        if (categoryVOList == null || categoryVOList.isEmpty()) return;
        for (CategoryVO categoryVO : categoryVOList) {
            if (categoryVO != null) {
                categoryIdList.add(categoryVO.getId());
                collectIds(categoryVO.getChildrenList(), categoryIdList);
            }
        }
    }

    @Override
    public void batchUpdateStatus(Integer[] ids, Integer status) {
        if (ids == null || ids.length == 0) {
             throw new CustomException(ExceptionEnum.WRONG_PARA);
        }
        boolean success = update(new LambdaUpdateWrapper<Product>()
                .in(Product::getId, Arrays.asList(ids))
                .set(Product::getStatus, status)
        );
        if (!success) {
            throw new CustomException(ExceptionEnum.UPDATE_FAILED);
        }
    }

    @Override
    public void increaseSale(Integer productId, Integer quantity) {
        if (productId == null || quantity == null || quantity <= 0) {
            throw new CustomException(ExceptionEnum.WRONG_PARA);
        }
        int affectedRows = baseMapper.increaseSale(productId, quantity);
        if (affectedRows != 1) {
            throw new CustomException(ExceptionEnum.UPDATE_FAILED);
        }
    }

    @Override
    public void decreaseStock(Integer productId, Integer quantity) {
        if (productId == null || quantity == null || quantity <= 0) {
            throw new CustomException(ExceptionEnum.WRONG_PARA);
        }
        int affectedRows = baseMapper.decreaseStock(productId, quantity);
        if (affectedRows != 1) {
            throw new CustomException(ExceptionEnum.PRODUCT_STOCK_NOT_ENOUGH);
        }
    }

    @Override
    public void increaseStock(Integer productId, Integer quantity) {
        if (productId == null || quantity == null || quantity <= 0) {
            throw new CustomException(ExceptionEnum.WRONG_PARA);
        }
        int affectedRows = baseMapper.increaseStock(productId, quantity);
        if (affectedRows != 1) {
            throw new CustomException(ExceptionEnum.UPDATE_FAILED);
        }
    }

    /**
     * 校验单个商品状态
     * @param product 商品
     * @param count 商品数量
     */
    @Override
    public void validProductStatusAndStock(Product product, Integer count) {
        // 商品不存在
        if (product == null) {
            throw new CustomException(ExceptionEnum.NOT_EXIST);
        }
        // 商品未上架
        if (product.getStatus() != ProductStatus.SALE) {
            throw new CustomException(ExceptionEnum.PRODUCT_STATUS_UNUSUAL);
        }
        // 判断库存
        if (count > product.getStock()) {
            throw new CustomException(ExceptionEnum.PRODUCT_STOCK_NOT_ENOUGH);
        }
    }
}
