package com.core.coreboot.cart.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.springframework.stereotype.Repository;
import org.apache.ibatis.annotations.Param;
import com.core.coreboot.cart.entity.Cart;
import com.core.coreboot.cart.vo.CartVO;

import java.util.List;

@Repository
public interface CartMapper extends BaseMapper<Cart> {
    List<CartVO> selectCartVOList(@Param("userId") Integer userId);
}