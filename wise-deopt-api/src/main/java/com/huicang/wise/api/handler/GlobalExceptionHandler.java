package com.huicang.wise.api.handler;

import com.huicang.wise.common.api.ApiResponse;
import com.huicang.wise.common.api.ErrorCode;
import com.huicang.wise.common.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

/**
 * 类功能描述：全局异常处理器
 *
 * @author xingchentye
 * @date 2026-01-19
 * @modified xingchentye 2026-01-19 统一转换异常为API响应
 */
@RestControllerAdvice
@lombok.extern.slf4j.Slf4j
public class GlobalExceptionHandler {

    /**
     * 方法功能描述：处理业务异常
     *
     * @param ex 业务异常对象
     * @return 统一响应结果
     */
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Void> handleBusinessException(BusinessException ex) {
        log.warn("业务异常: code={}, message={}", ex.getErrorCode().getCode(), ex.getMessage());
        return ApiResponse.failure(ex.getErrorCode(), ex.getMessage());
    }

    /**
     * 方法功能描述：处理参数校验异常
     *
     * @param ex 参数校验异常对象
     * @return 统一响应结果
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        log.warn("参数校验异常: {}", ex.getMessage());
        return ApiResponse.failure(ErrorCode.PARAM_ERROR, "请求参数校验失败");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        log.warn("请求体解析失败: {}", ex.getMessage());
        return ApiResponse.failure(ErrorCode.REQUEST_BODY_INVALID, ErrorCode.REQUEST_BODY_INVALID.getMessage());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public ApiResponse<Void> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException ex) {
        log.warn("不支持的请求方法: {}", ex.getMessage());
        return ApiResponse.failure(ErrorCode.REQUEST_METHOD_NOT_ALLOWED, ErrorCode.REQUEST_METHOD_NOT_ALLOWED.getMessage());
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ApiResponse<Void> handleResponseStatusException(ResponseStatusException ex) {
        log.warn("HTTP响应状态异常: status={}, message={}", ex.getStatusCode(), ex.getMessage());
        if (HttpStatus.NOT_FOUND.equals(ex.getStatusCode())) {
            return ApiResponse.failure(ErrorCode.REQUEST_NOT_FOUND, ErrorCode.REQUEST_NOT_FOUND.getMessage());
        }
        if (HttpStatus.METHOD_NOT_ALLOWED.equals(ex.getStatusCode())) {
            return ApiResponse.failure(ErrorCode.REQUEST_METHOD_NOT_ALLOWED, ErrorCode.REQUEST_METHOD_NOT_ALLOWED.getMessage());
        }
        return ApiResponse.failure(ErrorCode.SYSTEM_REQUEST_ERROR, ErrorCode.SYSTEM_REQUEST_ERROR.getMessage());
    }

    /**
     * 方法功能描述：处理系统未知异常
     *
     * @param ex 未知异常对象
     * @return 统一响应结果
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleException(Exception ex) {
        log.error("系统未知异常", ex);
        return ApiResponse.failure(ErrorCode.SYSTEM_ERROR, "系统异常，请联系管理员");
    }
}
