package com.huicang.wise.common.api;

import java.io.Serial;
import java.io.Serializable;

/**
 * 类功能描述：统一API响应结构
 *
 * @author xingchentye
 * @date 2026-01-19
 * @modified xingchentye 2026-01-19 初始化统一响应结构
 */
public class ApiResponse<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 方法功能描述：业务状态码
     */
    private String code;

    /**
     * 方法功能描述：提示信息
     */
    private String message;

    /**
     * 方法功能描述：数据内容
     */
    private T data;

    /**
     * 方法功能描述：无参构造函数
     *
     * @return 无
     */
    public ApiResponse() {
    }

    /**
     * 方法功能描述：全参构造函数
     *
     * @param code    业务状态码
     * @param message 提示信息
     * @param data    数据内容
     * @return 无
     */
    public ApiResponse(String code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    /**
     * 方法功能描述：创建成功响应
     *
     * @param data 数据内容
     * @return 成功响应对象
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMessage(), data);
    }

    /**
     * 方法功能描述：创建无数据成功响应
     *
     * @return 成功响应对象
     */
    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMessage(), null);
    }

    /**
     * 方法功能描述：根据错误码创建失败响应
     *
     * @param errorCode 错误码枚举
     * @return 失败响应对象
     */
    public static <T> ApiResponse<T> failure(ErrorCode errorCode) {
        return new ApiResponse<>(errorCode.getCode(), errorCode.getMessage(), null);
    }

    /**
     * 方法功能描述：根据错误码和自定义消息创建失败响应
     *
     * @param errorCode 错误码枚举
     * @param message   自定义错误信息
     * @return 失败响应对象
     */
    public static <T> ApiResponse<T> failure(ErrorCode errorCode, String message) {
        return new ApiResponse<>(errorCode.getCode(), message, null);
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}

