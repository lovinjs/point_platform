package com.core.coreboot.platform.auth.controller;

import com.core.coreboot.common.ApiRestResponse;
import com.core.coreboot.exception.CustomException;
import com.core.coreboot.exception.ExceptionEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.ArrayList;
import java.util.List;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(basePackages = "com.core.coreboot.platform")
public class PointPlatformExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(PointPlatformExceptionHandler.class);

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ApiRestResponse<Object>> handleCustomException(CustomException ex) {
        return ResponseEntity.status(statusFor(ex.getCode()))
                .body(ApiRestResponse.error(ex.getCode(), ex.getMessage()));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ResponseEntity<ApiRestResponse<Object>> handleValidationException(Exception ex) {
        BindingResult bindingResult = ex instanceof MethodArgumentNotValidException methodException
                ? methodException.getBindingResult()
                : ((BindException) ex).getBindingResult();
        List<String> messages = new ArrayList<>();
        for (ObjectError error : bindingResult.getAllErrors()) {
            String field = error instanceof FieldError fieldError ? fieldError.getField() : "";
            messages.add(field + error.getDefaultMessage());
        }
        String message = messages.isEmpty() ? ExceptionEnum.WRONG_PARA.getMsg() : String.join("; ", messages);
        return ResponseEntity.badRequest()
                .body(ApiRestResponse.error(ExceptionEnum.WRONG_PARA.getCode(), message));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiRestResponse<Object>> handleUnreadableRequest(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest().body(ApiRestResponse.error(ExceptionEnum.WRONG_PARA));
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<ApiRestResponse<Object>> handleDuplicateKey(DuplicateKeyException ex) {
        log.warn("积分平台数据唯一约束冲突", ex);
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiRestResponse.error(ExceptionEnum.PLATFORM_DATA_CONFLICT));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiRestResponse<Object>> handleUnexpectedException(Exception ex) {
        log.error("积分平台未处理异常", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiRestResponse.error(ExceptionEnum.SYSTEM_ERROR));
    }

    private HttpStatus statusFor(Integer code) {
        if (code == null) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        if (code.equals(ExceptionEnum.PLATFORM_ADMIN_LOGIN_FAILED.getCode())
                || code.equals(ExceptionEnum.PLATFORM_ADMIN_TOKEN_INVALID.getCode())
                || code.equals(ExceptionEnum.PLATFORM_CUSTOMER_TOKEN_INVALID.getCode())
                || code.equals(ExceptionEnum.PLATFORM_CUSTOMER_LOGIN_TICKET_INVALID.getCode())
                || code.equals(ExceptionEnum.PLATFORM_WECHAT_AUTH_STATE_INVALID.getCode())
                || code.equals(ExceptionEnum.PLATFORM_WECHAT_AUTH_FAILED.getCode())) {
            return HttpStatus.UNAUTHORIZED;
        }
        if (code.equals(ExceptionEnum.PLATFORM_ADMIN_ACCESS_DENIED.getCode())
                || code.equals(ExceptionEnum.PLATFORM_STORE_ACCESS_DENIED.getCode())
                || code.equals(ExceptionEnum.PLATFORM_CUSTOMER_ACCESS_DENIED.getCode())) {
            return HttpStatus.FORBIDDEN;
        }
        if (code.equals(ExceptionEnum.PLATFORM_ADMIN_SECURITY_NOT_CONFIGURED.getCode())
                || code.equals(ExceptionEnum.PLATFORM_CUSTOMER_SECURITY_NOT_CONFIGURED.getCode())
                || code.equals(ExceptionEnum.PLATFORM_CUSTOMER_PIN_CONFIGURATION_INVALID.getCode())
                || code.equals(ExceptionEnum.PLATFORM_WECHAT_AUTH_NOT_CONFIGURED.getCode())
                || code.equals(ExceptionEnum.PLATFORM_PHONE_VERIFICATION_NOT_CONFIGURED.getCode())
                || code.equals(ExceptionEnum.REDIS_CONNECTION_ERROR.getCode())) {
            return HttpStatus.SERVICE_UNAVAILABLE;
        }
        if (code.equals(ExceptionEnum.PLATFORM_WECHAT_API_UNAVAILABLE.getCode())
                || code.equals(ExceptionEnum.PLATFORM_SMS_SEND_FAILED.getCode())) {
            return HttpStatus.BAD_GATEWAY;
        }
        if (code.equals(ExceptionEnum.PLATFORM_PHONE_VERIFICATION_TOO_FREQUENT.getCode())
                || code.equals(ExceptionEnum.PLATFORM_PHONE_VERIFICATION_LIMIT_REACHED.getCode())
                || code.equals(ExceptionEnum.PLATFORM_CAPTCHA_REQUIRED.getCode())) {
            return HttpStatus.TOO_MANY_REQUESTS;
        }
        if (code.equals(ExceptionEnum.PLATFORM_CONSUME_PIN_LOCKED.getCode())
                || code.equals(ExceptionEnum.PLATFORM_PHONE_VERIFICATION_LOCKED.getCode())) {
            return HttpStatus.LOCKED;
        }
        if (code.equals(ExceptionEnum.PLATFORM_CUSTOMER_NOT_FOUND.getCode())
                || code.equals(ExceptionEnum.PLATFORM_STORE_NOT_FOUND.getCode())
                || code.equals(ExceptionEnum.PLATFORM_OPERATOR_NOT_FOUND.getCode())
                || code.equals(ExceptionEnum.PLATFORM_CONSUMPTION_ORDER_NOT_FOUND.getCode())) {
            return HttpStatus.NOT_FOUND;
        }
        if (code.equals(ExceptionEnum.PLATFORM_CONSUMPTION_ORDER_EXPIRED.getCode())) {
            return HttpStatus.GONE;
        }
        if (code.equals(ExceptionEnum.PLATFORM_PAYMENT_REFERENCE_USED.getCode())
                || code.equals(ExceptionEnum.PLATFORM_IDEMPOTENCY_CONFLICT.getCode())
                || code.equals(ExceptionEnum.PLATFORM_DATA_CONFLICT.getCode())
                || code.equals(ExceptionEnum.PLATFORM_POINT_BALANCE_INSUFFICIENT.getCode())
                || code.equals(ExceptionEnum.PLATFORM_CUSTOMER_HAS_PENDING_CONSUMPTION.getCode())
                || code.equals(ExceptionEnum.PLATFORM_CONSUME_PIN_ALREADY_SET.getCode())
                || code.equals(ExceptionEnum.PLATFORM_CONSUME_PIN_NOT_SET.getCode())
                || code.equals(ExceptionEnum.PLATFORM_CONSUME_PIN_UNCHANGED.getCode())
                || code.equals(ExceptionEnum.PLATFORM_PHONE_NOT_BOUND.getCode())
                || code.equals(ExceptionEnum.PLATFORM_PHONE_ALREADY_BOUND.getCode())
                || code.equals(ExceptionEnum.PLATFORM_PHONE_ALREADY_USED.getCode())
                || code.equals(ExceptionEnum.PLATFORM_PHONE_BIND_FAILED.getCode())
                || code.equals(ExceptionEnum.PLATFORM_CONSUMPTION_ORDER_NOT_PENDING.getCode())
                || code.equals(ExceptionEnum.PLATFORM_CONSUMPTION_CONFIRM_FAILED.getCode())) {
            return HttpStatus.CONFLICT;
        }
        return HttpStatus.BAD_REQUEST;
    }
}
