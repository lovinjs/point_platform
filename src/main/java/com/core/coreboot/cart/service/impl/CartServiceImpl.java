package com.core.coreboot.cart.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.core.coreboot.exception.CustomException;
import com.core.coreboot.exception.ExceptionEnum;
import com.core.coreboot.cart.mapper.CartMapper;
import com.core.coreboot.cart.entity.Cart;
import com.core.coreboot.cart.service.CartService;
import com.core.coreboot.product.service.ProductService;
import com.core.coreboot.utils.PriceUtils;
import org.springframework.stereotype.Service;
import com.core.coreboot.cart.vo.CartVO;
import com.core.coreboot.product.entity.Product;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartServiceImpl extends ServiceImpl<CartMapper, Cart> implements CartService {

    private final CartMapper cartMapper;

    private final ProductService productService;

    @Override
    public List<CartVO> list(Integer userId) {
        List<CartVO> cartVOList = cartMapper.selectCartVOList(userId);
        for (CartVO cartVO : cartVOList) {
            cartVO.setTotalPrice(PriceUtils.multiply(cartVO.getPrice(), cartVO.getQuantity()));
        }
        return cartVOList;
    }

    @Override
    public List<CartVO> add(Integer userId, Integer productId, Integer count) {
        Product dbProduct = productService.getProductDetailForAdmin(productId);
        productService.validProductStatusAndStock(dbProduct, count);
        Cart dbCart = getOne(new LambdaQueryWrapper<Cart>()
                .eq(Cart::getUserId, userId)
                .eq(Cart::getProductId, productId)
        );
        if (dbCart == null) {
            // 新增
            Cart cart = Cart.builder()
                    .userId(userId)
                    .productId(productId)
                    .quantity(count)
                    .build();
            boolean success = save(cart);
            if (!success) {
                throw new CustomException(ExceptionEnum.INSERT_FAILED);
            }
        } else {
            // 更新数量
            this.updateCartCount(dbCart, count);
        }
        return this.list(userId);
    }

    @Override
    public List<CartVO> update(Integer userId, Integer productId, Integer count) {
        Product dbProduct = productService.getProductDetailForAdmin(productId);
        productService.validProductStatusAndStock(dbProduct, count);
        Cart dbCart = getOne(new LambdaQueryWrapper<Cart>()
                .eq(Cart::getUserId, userId)
                .eq(Cart::getProductId, productId)
        );
        if (dbCart == null) {
            // 购物车记录不存在
            throw new CustomException(ExceptionEnum.NOT_EXIST);
        } else {
            // 更新数量
            this.updateCartCount(dbCart, count);
        }
        return this.list(userId);
    }

    @Override
    public List<CartVO> delete(Integer userId, Integer productId) {
        LambdaUpdateWrapper<Cart> wrapper = new LambdaUpdateWrapper<Cart>()
                .eq(Cart::getUserId, userId)
                .eq(Cart::getProductId, productId);
        boolean success = remove(wrapper);
        if (!success) {
            throw new CustomException(ExceptionEnum.DELETE_FAILED);
        }
        return this.list(userId);
    }

    @Override
    public void updateCartCount(Cart cart, Integer count) {
        count = count + cart.getQuantity();
        if (count <= 0) {
            throw new CustomException(ExceptionEnum.WRONG_PARA);
        }
        Cart newCart = Cart.builder()
                .id(cart.getId())
                .userId(cart.getUserId())
                .productId(cart.getProductId())
                .quantity(count)
                .build();
        boolean success = updateById(newCart);
        if (!success) {
            throw new CustomException(ExceptionEnum.UPDATE_FAILED);
        }
    }

    @Override
    public void validCartVOList(List<CartVO> cartVOList) {
        List<Integer> productIds = cartVOList.stream().map(CartVO::getProductId).collect(Collectors.toList());
        List<Product> productList = productService.getProductListByIds(productIds);
        Map<Integer, Product> productMap = productList.stream().collect(Collectors.toMap(Product::getId, Function.identity()));
        for (CartVO cartVO : cartVOList) {
            Product product = productMap.get(cartVO.getProductId());
            productService.validProductStatusAndStock(product, cartVO.getQuantity());
        }
    }

    @Override
    public void clearCartVOList(List<CartVO> cartVOList) {
        List<Integer> cartIds = cartVOList.stream().map(CartVO::getId).collect(Collectors.toList());
        boolean success = removeByIds(cartIds);
        if (!success) {
            throw new CustomException(ExceptionEnum.DELETE_FAILED);
        }
    }
}
