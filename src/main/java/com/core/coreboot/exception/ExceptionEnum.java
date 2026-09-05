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
    PLATFORM_CUSTOMER_NOT_FOUND(30001, "客户不存在"),
    PLATFORM_CUSTOMER_DISABLED(30002, "客户账户不可用"),
    PLATFORM_STORE_NOT_FOUND(30003, "门店不存在"),
    PLATFORM_STORE_UNAVAILABLE(30004, "门店当前不可操作"),
    PLATFORM_OPERATOR_NOT_FOUND(30005, "后台操作员不存在"),
    PLATFORM_OPERATOR_DISABLED(30006, "后台操作员账户不可用"),
    PLATFORM_STORE_ACCESS_DENIED(30007, "操作员没有该门店的数据权限"),
    PLATFORM_RECHARGE_AMOUNT_INVALID(30008, "充值金额必须是大于0的整元金额"),
    PLATFORM_PAYMENT_REFERENCE_REQUIRED(30009, "平台收款交易参考号不能为空"),
    PLATFORM_PAYMENT_REFERENCE_USED(30010, "该平台收款交易已经入账"),
    PLATFORM_IDEMPOTENCY_CONFLICT(30011, "幂等键已被不同的请求使用"),
    PLATFORM_POINT_ACCOUNT_ERROR(30012, "积分账户更新失败"),
    PLATFORM_RECHARGE_WRITE_FAILED(30013, "充值入账失败"),
    PLATFORM_INVALID_REQUEST(30014, "请求参数不符合积分平台规则"),
    PLATFORM_ADMIN_LOGIN_FAILED(30015, "后台用户名或密码错误"),
    PLATFORM_ADMIN_SECURITY_NOT_CONFIGURED(30016, "后台认证密钥未正确配置"),
    PLATFORM_ADMIN_TOKEN_INVALID(30017, "后台登录凭证无效或已过期"),
    PLATFORM_ADMIN_AUTHORITY_INVALID(30018, "后台账号权限配置异常"),
    PLATFORM_ADMIN_BOOTSTRAP_INVALID(30019, "超级管理员初始化配置无效"),
    PLATFORM_ADMIN_ACCESS_DENIED(30020, "后台账号无权执行该操作"),
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
