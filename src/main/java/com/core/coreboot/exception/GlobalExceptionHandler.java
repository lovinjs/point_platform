package com.core.coreboot.exception;

import com.core.coreboot.common.ApiRestResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.mybatis.spring.MyBatisSystemException;

import java.util.ArrayList;
import java.util.List;

@ControllerAdvice
public class GlobalExceptionHandler {
    private final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(Exception.class)
    @ResponseBody
    public Object handleException(Exception ex){
        // 处理默认异常
        log.error("Default Exception: ", ex);
        return ApiRestResponse.error(ExceptionEnum.SYSTEM_ERROR);
    }

    @ExceptionHandler(CustomException.class)
    @ResponseBody
    public Object handleCustomException(CustomException ex){
        // 处理自定义异常
        log.error("Custom Exception: ", ex);
        return ApiRestResponse.error(ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler(RedisConnectionFailureException.class)
    @ResponseBody
    public Object handleRedisConnectionFailureException(Exception ex){
        // 处理 Redis 连接异常
        log.error("Redis Connection Exception: ", ex);
        return ApiRestResponse.error(ExceptionEnum.REDIS_CONNECTION_ERROR);
    }
      
    @ExceptionHandler(MyBatisSystemException.class)
    @ResponseBody
    public Object handleMyBatisSystemException(MyBatisSystemException ex){
        // 处理 MySQL 连接异常
        log.error("MySQL Connection Exception: ", ex);
        return ApiRestResponse.error(ExceptionEnum.MYSQL_CONNECTION_ERROR);
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    @ResponseBody
    public Object handleParamsNotValidException(Exception ex){
        String exceptionType = ex.getClass().getSimpleName();
        log.error("{}: ", exceptionType, ex);
        // 处理参数校验异常信息
        BindingResult bindingResult = null;
        if (ex instanceof MethodArgumentNotValidException) {
            // RequestBody 类型参数
            bindingResult = ((MethodArgumentNotValidException) ex).getBindingResult();
        } else if (ex instanceof BindException) {
            // RequestParam 类型参数
            bindingResult = ((BindException) ex).getBindingResult();
        }
        
        List<String> list = new ArrayList<>();
        if (bindingResult != null && bindingResult.hasErrors()) {
            List<ObjectError> allErrors = bindingResult.getAllErrors();
            for (ObjectError objectError : allErrors) {
                // 获取参数字段名称
                String fieldName = (objectError instanceof FieldError)
                    ? ((FieldError) objectError).getField()
                    : "";
                String errorMessage = fieldName + objectError.getDefaultMessage();
                list.add(errorMessage);
            }
        }
        if (list.isEmpty()) {
            return ApiRestResponse.error(ExceptionEnum.WRONG_PARA);
        }
        return ApiRestResponse.error(ExceptionEnum.WRONG_PARA.getCode(), String.join("; ", list));
    }
}
