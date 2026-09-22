/*
 * AUTO-GENERATED FROM 后端异常码对照表.csv — DO NOT EDIT
 * 生成器：WiseDeoptServer/tools/gen/gen-errorcodes.ps1（要改错误码请先改 CSV/manifest 再重新生成）
 *
 * 格式约束：本文件必须通过 `mvn -B spotless:check`（google-java-format），
 * 因此枚举体首行不留空行（2026-02-27 实测：留空行会让 verify 阶段 BUILD FAILURE）。
 */
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
