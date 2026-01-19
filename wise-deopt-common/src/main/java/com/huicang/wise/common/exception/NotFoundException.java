package com.huicang.wise.common.exception;

import com.huicang.wise.common.api.ErrorCode;

/**
 * 类功能描述：资源未找到异常
 *
 * @author xingchentye
 * @date 2026-01-19
 * @modified xingchentye 2026-01-19 定义资源未找到异常
 */
public class NotFoundException extends BusinessException {

    /**
     * 方法功能描述：构造资源未找到异常
     *
     * @param message 错误信息
     * @return 无
     */
    public NotFoundException(String message) {
        super(ErrorCode.NOT_FOUND, message);
    }
}

