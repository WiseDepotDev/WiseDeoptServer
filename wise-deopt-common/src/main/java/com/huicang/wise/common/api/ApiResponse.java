package com.huicang.wise.common.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.io.Serial;
import java.io.Serializable;

/**
 * 统一API响应结构
 *
 * @author WiseDepot
 * @version 0.0.29
 * @since 2026-02-27
 */
public class ApiResponse<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 业务状态码
     */
    private String code;

    /**
     * 提示信息
     */
    private String message;

    /**
     * 数据内容
     */
    private T data;

    /**
     * 请求ID
     */
    private String requestId;

    /**
     * HTTP状态码
     */
    @JsonIgnore
    private int httpStatus;

    /**
     * 错误码(异常时)
     */
    private String errorCode;

    /**
     * 无参构造函数
     */
    public ApiResponse() {
    }

    /**
     * 全参构造函数
     *
     * @param code      业务状态码
     * @param message   提示信息
     * @param data      数据内容
     * @param requestId 请求ID
     */
    public ApiResponse(String code, String message, T data, String requestId) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.requestId = requestId;
    }

    /**
     * 创建成功响应
     *
     * @param data 数据内容
     * @return 成功响应对象
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMessage(), data, null);
    }

    /**
     * 创建成功响应（带请求ID）
     *
     * @param data      数据内容
     * @param requestId 请求ID
     * @return 成功响应对象
     */
    public static <T> ApiResponse<T> success(T data, String requestId) {
        return new ApiResponse<>(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMessage(), data, requestId);
    }

    /**
     * 创建无数据成功响应
     *
     * @return 成功响应对象
     */
    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMessage(), null, null);
    }

    /**
     * 创建无数据成功响应（带请求ID）
     *
     * @param requestId 请求ID
     * @return 成功响应对象
     */
    public static <T> ApiResponse<T> success(String requestId) {
        return new ApiResponse<>(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMessage(), null, requestId);
    }

    /**
     * 根据错误码创建失败响应
     *
     * @param errorCode 错误码枚举
     * @return 失败响应对象
     */
    public static <T> ApiResponse<T> failure(ErrorCode errorCode) {
        return new ApiResponse<>(errorCode.getCode(), errorCode.getMessage(), null, null);
    }

    /**
     * 根据错误码和自定义消息创建失败响应
     *
     * @param errorCode 错误码枚举
     * @param message   自定义错误信息
     * @return 失败响应对象
     */
    public static <T> ApiResponse<T> failure(ErrorCode errorCode, String message) {
        return new ApiResponse<>(errorCode.getCode(), message, null, null);
    }

    /**
     * 根据错误码和自定义消息创建失败响应（带请求ID）
     *
     * @param errorCode 错误码枚举
     * @param message   自定义错误信息
     * @param requestId 请求ID
     * @return 失败响应对象
     */
    public static <T> ApiResponse<T> failure(ErrorCode errorCode, String message, String requestId) {
        ApiResponse<T> response = new ApiResponse<>(errorCode.getCode(), message, null, requestId);
        response.setErrorCode(errorCode.getCode());
        return response;
    }

    /**
     * 根据错误码创建失败响应（带错误码字段）
     *
     * @param errorCode 错误码枚举
     * @return 失败响应对象
     */
    public static <T> ApiResponse<T> error(ErrorCode errorCode) {
        ApiResponse<T> response = new ApiResponse<>(errorCode.getCode(), errorCode.getMessage(), null, null);
        response.setErrorCode(errorCode.getCode());
        return response;
    }

    /**
     * 根据错误码和自定义消息创建失败响应（带错误码字段）
     *
     * @param errorCode 错误码枚举
     * @param message   自定义错误信息
     * @return 失败响应对象
     */
    public static <T> ApiResponse<T> error(ErrorCode errorCode, String message) {
        ApiResponse<T> response = new ApiResponse<>(errorCode.getCode(), message, null, null);
        response.setErrorCode(errorCode.getCode());
        return response;
    }

    /**
     * 根据错误码和自定义消息创建失败响应（带错误码字段和请求ID）
     *
     * @param errorCode 错误码枚举
     * @param message   自定义错误信息
     * @param requestId 请求ID
     * @return 失败响应对象
     */
    public static <T> ApiResponse<T> error(ErrorCode errorCode, String message, String requestId) {
        ApiResponse<T> response = new ApiResponse<>(errorCode.getCode(), message, null, requestId);
        response.setErrorCode(errorCode.getCode());
        return response;
    }

    /**
     * 根据HTTP状态码和自定义消息创建失败响应
     *
     * @param httpStatus HTTP状态码
     * @param message   自定义错误信息
     * @return 失败响应对象
     */
    public static <T> ApiResponse<T> error(int httpStatus, String message) {
        String code = String.valueOf(httpStatus);
        ApiResponse<T> response = new ApiResponse<>(code, message, null, null);
        response.setErrorCode(code);
        response.setHttpStatus(httpStatus);
        return response;
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

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public void setHttpStatus(int httpStatus) {
        this.httpStatus = httpStatus;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    /**
     * 判断是否成功
     *
     * @return true表示成功
     */
    @JsonIgnore
    public boolean isSuccess() {
        return ErrorCode.SUCCESS.getCode().equals(this.code);
    }
}
