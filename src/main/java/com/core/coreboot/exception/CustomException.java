package com.core.coreboot.exception;

public class CustomException extends RuntimeException {
    private final Integer code;
    private final String message;

    public CustomException(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    public CustomException(ExceptionEnum exceptionEnum) {
        this(exceptionEnum.getCode(), exceptionEnum.getMsg());
    }

    public Integer getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
