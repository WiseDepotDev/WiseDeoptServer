package com.huicang.wise.common.exception;

import com.huicang.wise.common.api.ErrorCode;

import java.util.List;

/**
 * 参数校验异常
 *
 * @author WiseDepot
 * @version 0.0.28
 * @since 2026-02-27
 */
public class ValidationException extends BusinessException {

    private final List<String> fieldErrors;

    /**
     * 根据错误码构造参数校验异常
     *
     * @param errorCode 错误码枚举
     */
    public ValidationException(ErrorCode errorCode) {
        super(errorCode);
        this.fieldErrors = null;
    }

    /**
     * 根据错误码和自定义消息构造参数校验异常
     *
     * @param errorCode 错误码枚举
     * @param message   自定义错误信息
     */
    public ValidationException(ErrorCode errorCode, String message) {
        super(errorCode, message);
        this.fieldErrors = null;
    }

    /**
     * 根据错误码和字段错误列表构造参数校验异常
     *
     * @param errorCode  错误码枚举
     * @param fieldErrors 字段错误列表
     */
    public ValidationException(ErrorCode errorCode, List<String> fieldErrors) {
        super(errorCode);
        this.fieldErrors = fieldErrors;
    }

    /**
     * 根据错误码、自定义消息和字段错误列表构造参数校验异常
     *
     * @param errorCode  错误码枚举
     * @param message    自定义错误信息
     * @param fieldErrors 字段错误列表
     */
    public ValidationException(ErrorCode errorCode, String message, List<String> fieldErrors) {
        super(errorCode, message);
        this.fieldErrors = fieldErrors;
    }

    /**
     * 获取字段错误列表
     *
     * @return 字段错误列表
     */
    public List<String> getFieldErrors() {
        return fieldErrors;
    }
}
