/*
 * AUTO-GENERATED FROM 后端异常码对照表.csv — DO NOT EDIT
 * 生成器：WiseDeoptServer/tools/gen/gen-errorcodes.ps1（要改错误码请先改 CSV/manifest 再重新生成）
 *
 * 格式约束：本文件必须通过 `./gradlew :app:ktlintCheck`（APP 门禁的一部分），
 * 因此参数列表多行 + 尾随逗号、类体首行不留空、`fromCode` 用表达式体。
 */
package com.huicang.wise.network

enum class ErrorCode(
    val code: String,
    val message: String,
    val httpStatus: Int,
) {
@@CONSTANTS@@

    companion object {
        fun fromCode(code: String?): ErrorCode = values().find { it.code == code } ?: SYSTEM_ERROR
    }
}
