package com.huicang.wise.common.exception;

import com.huicang.wise.common.api.ErrorCode;

/**
 * 业务异常基类
 *
 * @author WiseDepot
 * @version 0.0.28
 * @since 2026-02-27
 */
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    /**
     * 根据错误码构造业务异常
     *
     * @param errorCode 错误码枚举
     */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    /**
     * 根据错误码和自定义消息构造业务异常
     *
     * @param errorCode 错误码枚举
     * @param message   自定义错误信息
     */
    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * 根据错误码、自定义消息和原因构造业务异常
     *
     * @param errorCode 错误码枚举
     * @param message   自定义错误信息
     * @param cause     异常原因
     */
    public BusinessException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    /**
     * 获取错误码
     *
     * @return 错误码枚举
     */
    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
