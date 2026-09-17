package com.huicang.wise.network

enum class ErrorCode(val code: String, val message: String, val httpStatus: Int) {

@@CONSTANTS@@

    companion object {
        fun fromCode(code: String?): ErrorCode {
            return values().find { it.code == code } ?: SYSTEM_ERROR
        }
    }
}
