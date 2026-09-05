package com.core.coreboot.product.constant;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.core.coreboot.product.entity.Product;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ProductSortEnum {

    DEFAULT(0, "默认排序") {
        @Override
        public void handle(LambdaQueryWrapper<Product> wrapper) {
            // 创建时间倒序 + ID倒序
            wrapper.orderByDesc(Product::getCreateTime);
            wrapper.orderByDesc(Product::getId);
        }
    },

    PRICE_ASC(1, "价格升序") {
        @Override
        public void handle(LambdaQueryWrapper<Product> wrapper) {
            // 价格升序 + 库存降序 + ID倒序
            wrapper.orderByAsc(Product::getPrice);
            wrapper.orderByDesc(Product::getStock);
            wrapper.orderByDesc(Product::getId);
        }
    },

    PRICE_DESC(2, "价格降序") {
        @Override
        public void handle(LambdaQueryWrapper<Product> wrapper) {
            // 价格降序 + 库存降序 + ID倒序
            wrapper.orderByDesc(Product::getPrice);
            wrapper.orderByDesc(Product::getStock);
            wrapper.orderByDesc(Product::getId);
        }
    },

    SALE_ASC(3, "销量升序") {
        @Override
        public void handle(LambdaQueryWrapper<Product> wrapper) {
            // 销量升序 + 库存降序 + ID倒序
            wrapper.orderByAsc(Product::getTotalSale);
            wrapper.orderByDesc(Product::getStock);
            wrapper.orderByDesc(Product::getId);
        }
    },

    SALE_DESC(4, "销量降序") {
        @Override
        public void handle(LambdaQueryWrapper<Product> wrapper) {
            // 销量降序 + 库存降序 + ID倒序
            wrapper.orderByDesc(Product::getTotalSale);
            wrapper.orderByDesc(Product::getStock);
            wrapper.orderByDesc(Product::getId);
        }
    },

    STOCK_ASC(5, "库存升序") {
        @Override
        public void handle(LambdaQueryWrapper<Product> wrapper) {
            // 库存升序 + ID倒序
            wrapper.orderByAsc(Product::getStock);
            wrapper.orderByDesc(Product::getId);
        }
    },

    STOCK_DESC(6, "库存降序") {
        @Override
        public void handle(LambdaQueryWrapper<Product> wrapper) {
            // 库存降序 + ID倒序
            wrapper.orderByDesc(Product::getStock);
            wrapper.orderByDesc(Product::getId);
        }
    };

    private final Integer code;
    private final String desc;

    public abstract void handle(LambdaQueryWrapper<Product> wrapper);

    public static ProductSortEnum getByCode(Integer code) {
        for (ProductSortEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return DEFAULT;
    }
}
