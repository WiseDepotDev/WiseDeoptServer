package com.huicang.wise.common.api;

/**
 * 业务错误码枚举
 *
 * @author WiseDepot
 * @version 0.0.28
 * @since 2026-02-27
 */
public enum ErrorCode {

@@CONSTANTS@@

    private final String code;

    private final String message;

    private final int httpStatus;

    ErrorCode(String code, String message, int httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public int getHttpStatus() {
        return httpStatus;
    }
}
