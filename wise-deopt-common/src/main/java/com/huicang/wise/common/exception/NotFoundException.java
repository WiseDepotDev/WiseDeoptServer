package com.huicang.wise.common.exception;

import com.huicang.wise.common.api.ErrorCode;

/**
 * 资源未找到异常
 *
 * @author WiseDepot
 * @version 0.0.28
 * @since 2026-02-27
 */
public class NotFoundException extends BusinessException {

    /**
     * 根据错误码构造资源未找到异常
     *
     * @param errorCode 错误码枚举
     */
    public NotFoundException(ErrorCode errorCode) {
        super(errorCode);
    }

    /**
     * 根据错误码和自定义消息构造资源未找到异常
     *
     * @param errorCode 错误码枚举
     * @param message   自定义错误信息
     */
    public NotFoundException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    /**
     * 根据资源类型和ID构造资源未找到异常
     *
     * @param resourceType 资源类型
     * @param resourceId   资源ID
     */
    public NotFoundException(String resourceType, Object resourceId) {
        super(ErrorCode.NOT_FOUND, String.format("%s不存在: %s", resourceType, resourceId));
    }
}
