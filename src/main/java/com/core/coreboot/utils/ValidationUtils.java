package com.core.coreboot.utils;

import com.core.coreboot.exception.CustomException;
import com.core.coreboot.exception.ExceptionEnum;
import org.springframework.util.StringUtils;

/**
 * 验证工具类
 */
public class ValidationUtils {
    /**
     * 验证用户参数
     * @param userName 用户名
     * @param password 密码
     */
    public static void verifyUserParam(String userName, String password) {
        if (!StringUtils.hasText(userName)) {
            throw new CustomException(ExceptionEnum.NEED_USER_NAME);
        }
        if (!StringUtils.hasText(password)) {
            throw new CustomException(ExceptionEnum.NEED_PASSWORD);
        }
        if (password.length() < 6) {
            throw new CustomException(ExceptionEnum.PASSWORD_TOO_SHORT);
        }
    }
}
