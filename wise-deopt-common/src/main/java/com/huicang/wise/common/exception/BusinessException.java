package com.huicang.wise.common.exception;

import com.huicang.wise.common.api.ErrorCode;

/**
 * 类功能描述：业务异常基类
 *
 * @author xingchentye
 * @date 2026-01-19
 * @modified xingchentye 2026-01-19 定义业务异常结构
 */
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    /**
     * 方法功能描述：根据错误码构造业务异常
     *
     * @param errorCode 错误码枚举
     * @return 无
     */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    /**
     * 方法功能描述：根据错误码和自定义消息构造业务异常
     *
     * @param errorCode 错误码枚举
     * @param message   自定义错误信息
     * @return 无
     */
    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * 方法功能描述：获取错误码
     *
     * @return 错误码枚举
     */
    public ErrorCode getErrorCode() {
        return errorCode;
    }
}

