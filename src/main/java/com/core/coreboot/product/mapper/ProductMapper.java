package com.core.coreboot.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.springframework.stereotype.Repository;
import com.core.coreboot.product.entity.Product;

@Repository
public interface ProductMapper extends BaseMapper<Product> {

    @Update("UPDATE t_product SET sale = sale + #{quantity} WHERE id = #{productId}")
    int increaseSale(@Param("productId") Integer productId, @Param("quantity") Integer quantity);

    @Update("UPDATE t_product SET stock = stock - #{quantity} WHERE id = #{productId} AND stock >= #{quantity} AND is_deleted = 0")
    int decreaseStock(@Param("productId") Integer productId, @Param("quantity") Integer quantity);

    @Update("UPDATE t_product SET stock = stock + #{quantity} WHERE id = #{productId}")
    int increaseStock(@Param("productId") Integer productId, @Param("quantity") Integer quantity);
}