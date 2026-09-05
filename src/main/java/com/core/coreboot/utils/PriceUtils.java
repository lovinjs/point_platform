package com.core.coreboot.utils;

import com.core.coreboot.exception.CustomException;
import com.core.coreboot.exception.ExceptionEnum;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class PriceUtils {
    private static final int SCALE = 2;
    private static final RoundingMode MODE = RoundingMode.HALF_UP;

    /**
     * 加法
     * @param price1 价格1
     * @param price2 价格2
     * @return 加法结果，四舍五入，保留两位小数
     */
    public static BigDecimal add(BigDecimal price1, BigDecimal price2) {
        BigDecimal v1 = price1 == null ? BigDecimal.ZERO : price1;
        BigDecimal v2 = price2 == null ? BigDecimal.ZERO : price2;
        return v1.add(v2).setScale(SCALE, MODE);
    }

    /**
     * 减法
     * @param price1 价格1
     * @param price2 价格2
     * @return 减法结果，四舍五入，保留两位小数
     */
    public static BigDecimal subtract(BigDecimal price1, BigDecimal price2) {
        BigDecimal v1 = price1 == null ? BigDecimal.ZERO : price1;
        BigDecimal v2 = price2 == null ? BigDecimal.ZERO : price2;
        return v1.subtract(v2).setScale(SCALE, MODE);
    }

    /**
     * 乘法
     * @param price 价格
     * @param quantity 数量
     * @return 乘法结果，四舍五入，保留两位小数
     */
    public static BigDecimal multiply(BigDecimal price, BigDecimal quantity) {
        if (price == null || quantity == null) return BigDecimal.ZERO.setScale(SCALE, MODE);
        return price.multiply(quantity).setScale(SCALE, MODE);
    }

    /**
     * 乘法 - 整数数量
     * @param price 价格
     * @param quantity 数量
     * @return 乘法结果，四舍五入，保留两位小数
     */
    public static BigDecimal multiply(BigDecimal price, Integer quantity) {
        if (price == null || quantity == null) return BigDecimal.ZERO.setScale(SCALE, MODE);
        return multiply(price, BigDecimal.valueOf(quantity));
    }

    /**
     * 乘法 - 浮点数数量
     * @param price 价格
     * @param quantity 数量
     * @return 乘法结果，四舍五入，保留两位小数
     */
    public static BigDecimal multiply(BigDecimal price, Double quantity) {
        if (price == null || quantity == null) return BigDecimal.ZERO.setScale(SCALE, MODE);
        return multiply(price, BigDecimal.valueOf(quantity));
    }

    /**
     * 乘法 - 字符串数量
     * @param price 价格
     * @param quantity 数量
     * @return 乘法结果，四舍五入，保留两位小数
     */
    public static BigDecimal multiply(BigDecimal price, String quantity) {
        if (price == null || quantity == null || quantity.trim().isEmpty()) {
            return BigDecimal.ZERO.setScale(SCALE, MODE);
        }
        try {
            return multiply(price, new BigDecimal(quantity.trim()));
        } catch (Exception e) {
            throw new CustomException(ExceptionEnum.WRONG_PARA);
        }
    }

    /**
     * 判断价格是否大于 0
     */
    public static boolean gt0(BigDecimal price) {
        return price != null && price.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * 判断价格是否大于等于 0
     */
    public static boolean ge0(BigDecimal price) {
        return price != null && price.compareTo(BigDecimal.ZERO) >= 0;
    }

    /**
     * 判断价格是否小于 0
     */
    public static boolean lt0(BigDecimal price) {
        return price != null && price.compareTo(BigDecimal.ZERO) < 0;
    }

    /**
     * 判断价格是否小于等于 0
     */
    public static boolean le0(BigDecimal price) {
        return price != null && price.compareTo(BigDecimal.ZERO) <= 0;
    }

    /**
     * 判断价格是否等于 0
     */
    public static boolean eq0(BigDecimal price) {
        return price == null || price.compareTo(BigDecimal.ZERO) == 0;
    }

    /**
     * 比较两个价格是否相等
     */
    public static boolean equals(BigDecimal price1, BigDecimal price2) {
        if (price1 == null && price2 == null) return true;
        if (price1 == null || price2 == null) return false;
        return price1.compareTo(price2) == 0;
    }

    /**
     * 格式化价格
     */
    public static BigDecimal format(BigDecimal price) {
        if (price == null) return BigDecimal.ZERO.setScale(SCALE, MODE);
        return price.setScale(SCALE, MODE);
    }
}
