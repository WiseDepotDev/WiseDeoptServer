package com.huicang.wise.common.exception;

import com.huicang.wise.common.api.ErrorCode;

/**
 * 认证异常
 *
 * @author WiseDepot
 * @version 0.0.28
 * @since 2026-02-27
 */
public class AuthenticationException extends BusinessException {

    /**
     * 根据错误码构造认证异常
     *
     * @param errorCode 错误码枚举
     */
    public AuthenticationException(ErrorCode errorCode) {
        super(errorCode);
    }

    /**
     * 根据错误码和自定义消息构造认证异常
     *
     * @param errorCode 错误码枚举
     * @param message   自定义错误信息
     */
    public AuthenticationException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    /**
     * 根据错误码、自定义消息和原因构造认证异常
     *
     * @param errorCode 错误码枚举
     * @param message   自定义错误信息
     * @param cause     异常原因
     */
    public AuthenticationException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}
