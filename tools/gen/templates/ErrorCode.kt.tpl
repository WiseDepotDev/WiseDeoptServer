/*
 * AUTO-GENERATED FROM 后端异常码对照表.csv — DO NOT EDIT
 * 生成器：WiseDeoptServer/tools/gen/gen-errorcodes.ps1（要改错误码请先改 CSV/manifest 再重新生成）
 */
package com.huicang.wise.network

enum class ErrorCode(val code: String, val message: String, val httpStatus: Int) {

@@CONSTANTS@@

    companion object {
        fun fromCode(code: String?): ErrorCode {
            return values().find { it.code == code } ?: SYSTEM_ERROR
        }
    }
}
