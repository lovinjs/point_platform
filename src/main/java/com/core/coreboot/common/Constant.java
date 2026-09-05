package com.core.coreboot.common;

import com.core.coreboot.exception.CustomException;
import com.core.coreboot.exception.ExceptionEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class Constant {
    public static final String SALT = "dHaVAAVj;oYD]";
    public static final String JWT_KEY = "jwt_user_key";
    public static final String USER_ID = "user_id";
    public static final String USER_NAME = "user_name";
    public static final String USER_SIGNATURE = "user_signature";
    public static final String USER_ROLE = "user_role";
    // 1 天
    public static final Long EXPIRE_TIME_MILLIS = 24 * 60 * 60 * 1000L;

    public static String UPLOAD_FILE_PATH;
    public static String UPLOAD_IMAGE_PATH;
    public static String QRCODE_GENERATE_PATH;

    public static final List<String> ALLOWED_IMAGE_EXTENSIONS = Arrays.asList(
        "jpg", "jpeg", "png", "gif", "bmp", "webp"
    );

    @Getter
    @AllArgsConstructor
    public enum OrderStatusEnum {
        CANCELLED(0, "已取消"),
        UNPAID(10, "未付款"),
        PAID(20, "已付款"),
        SHIPPED(30, "已发货"),
        DONE(40, "订单完成");

        private final int code;
        private final String value;

        public static OrderStatusEnum getByCode(Integer code) {
            for (OrderStatusEnum orderStatusEnum : OrderStatusEnum.values()) {
                if (orderStatusEnum.getCode() == code) {
                    return orderStatusEnum;
                }
            }
            throw new CustomException(ExceptionEnum.NO_ENUM);
        }

    }

    @Value("${upload.file.path}")
    public void setUploadFilePath(String uploadFilePath) {
        UPLOAD_FILE_PATH = uploadFilePath;
    }

    @Value("${upload.image.path}")
    public void setUploadImagePath(String uploadImagePath) {
        UPLOAD_IMAGE_PATH = uploadImagePath;
    }

    @Value("${qrcode.generate.path}")
    public void setQrcodeGeneratePath(String qrcodeGeneratePath) {
        QRCODE_GENERATE_PATH = qrcodeGeneratePath;
    }
}
