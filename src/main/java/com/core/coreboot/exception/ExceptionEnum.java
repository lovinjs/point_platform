package com.core.coreboot.exception;

/**
 * 异常枚举
 */
public enum ExceptionEnum {
    NEED_USER_NAME(10001, "用户名不能为空"),
    NEED_PASSWORD(10002, "密码不能为空"),
    PASSWORD_TOO_SHORT(10003, "密码长度不能小于6位"),
    NAME_EXISTED(10004, "名称重复"),
    INSERT_FAILED(10005, "新增失败，请重试"),
    WRONG_USERNAME_OR_PASSWORD(10006, "用户名或密码错误"),
    NEED_LOGGED(10007, "用户未登录"),
    UPDATE_FAILED(10008, "更新失败"),
    NEED_ADMIN(10009, "无管理员权限"),
    NEED_PARA(10010, "参数不能为空"),
    WRONG_PARA(10011, "参数错误"),
    DELETE_FAILED(10012, "删除失败"),
    MKDIR_FAILED(10013, "文件夹创建失败"),
    FILE_UPLOAD_FAILED(10014, "文件上传失败"),
    IMAGE_UPLOAD_FAILED(10015, "图片上传失败"),
    ONLY_IMAGE_ALLOWED(10016, "只允许上传图片格式"),
    PRODUCT_STATUS_UNUSUAL(10017, "商品状态异常"),
    PRODUCT_STOCK_NOT_ENOUGH(10018, "商品库存不足"),
    CART_NOT_SELECTED(10019, "未勾选商品"),
    NO_ENUM(10020, "未找到对应枚举"),
    NO_ORDER(10021, "订单不存在"),
    NOT_YOUR_ORDER(10022, "订单不属于你"),
    WRONG_ORDER_STATUS(10023, "订单状态异常"),
    TOKEN_EXPIRED(10024, "token已过期"),
    TOKEN_WRONG(10025, "token解析异常"),
    NOT_EXIST(10026, "指定查询不存在"),
    SYSTEM_ERROR(20000, "系统异常"),
    REDIS_CONNECTION_ERROR(20001, "Redis缓存服务暂不可用"),
    MYSQL_CONNECTION_ERROR(20002, "MySQL数据库服务暂不可用");

    Integer code;
    String msg;

    ExceptionEnum(Integer code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}
