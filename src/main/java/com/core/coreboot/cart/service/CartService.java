package com.core.coreboot.cart.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.core.coreboot.cart.entity.Cart;
import com.core.coreboot.cart.vo.CartVO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface CartService extends IService<Cart> {

    List<CartVO> list(Integer userId);

    List<CartVO> add(Integer userId, Integer productId, Integer count);

    List<CartVO> update(Integer userId, Integer productId, Integer count);

    List<CartVO> delete(Integer userId, Integer productId);

    /**
     * 更新购物车商品数量
     */
    void updateCartCount(Cart cart, Integer count);

    /**
     * 校验购物车商品列表
     * @param cartVOList 购物车商品列表
     */
    void validCartVOList(List<CartVO> cartVOList);

    /**
     * 清空购物车商品列表
     * @param cartVOList 购物车商品列表
     */
    void clearCartVOList(List<CartVO> cartVOList);
}
