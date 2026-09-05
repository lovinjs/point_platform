package com.core.coreboot.common;

import com.core.coreboot.exception.ExceptionEnum;

/**
 * 统一返回对象
 */
public class ApiRestResponse<T> {
    private Integer code;
    private String msg;
    private T data;
    private static final int OK_CODE = 200;
    private static final String OK_MSG = "SUCCESS";

    public ApiRestResponse(Integer code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    public ApiRestResponse(Integer code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    /**
     * 默认无参请求成功
     */
    public ApiRestResponse() {
        this(OK_CODE, OK_MSG);
    }

    /**
     * 静态成功方法
     * @return 成功响应对象
     * @param <T>
     */
    public static <T> ApiRestResponse<T> success() {
        return new ApiRestResponse<>();
    }

    /**
     * 重载的静态成功方法
     * @param result 处理结果
     * @return 包含处理结果的成功响应对象
     * @param <T>
     */
    public static <T> ApiRestResponse<T> success(T result) {
        ApiRestResponse<T> response = new ApiRestResponse<T>();
        response.setData(result);
        return response;
    }

    public static <T> ApiRestResponse<T> error(ExceptionEnum ex) {
        return new ApiRestResponse<>(ex.getCode(), ex.getMsg());
    }

    public static <T> ApiRestResponse<T> error(Integer code, String msg) {
        return new ApiRestResponse<>(code, msg);
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

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "ApiRestResponse{" +
                "code=" + code +
                ", msg='" + msg + '\'' +
                ", data=" + data +
                '}';
    }
}
