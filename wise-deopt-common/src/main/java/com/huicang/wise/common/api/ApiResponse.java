package com.huicang.wise.common.api;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.io.Serial;
import java.io.Serializable;

/**
 * 统一 API 响应结构（信封 payload 部分）。
 *
 * <p>与 {@link com.huicang.wise.common.protocol.Packet} 配合，构成对外统一报文：
 * <pre>
 * {
 *   "header":  { "request_id": "...", "packet_type": "...", "timestamp": 0 },
 *   "payload": { "code": "RES-0000", "message": "处理成功", "data": {}, "errorCode": null }
 * }
 * </pre>
 *
 * <p>契约要点（STD-CONTRACT-01）：
 * <ul>
 *   <li>{@code request_id} 由 header 唯一承载，本类不再提供 requestId 字段，避免同一语义两处存在；</li>
 *   <li>{@code code} 为业务码字符串，成功固定 {@code RES-0000}，业务成败只看该字段；</li>
 *   <li>{@code errorCode} 仅在失败时出现，值与《后端异常码对照表》一致；失败工厂方法统一写入该字段。</li>
 * </ul>
 *
 * @author WiseDepot
 * @version 0.0.30
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
     * HTTP 状态码（不参与 JSON 序列化，仅用于设置响应状态）
     */
    @JsonIgnore
    private int httpStatus;

    /**
     * 错误码（仅失败时出现，取自《后端异常码对照表》）
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
     * @param errorCode 错误码（成功时为 null）
     */
    public ApiResponse(String code, String message, T data, String errorCode) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.errorCode = errorCode;
    }

    /**
     * 创建成功响应。
     *
     * @param data 数据内容
     * @param <T>  数据类型
     * @return 成功响应对象
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMessage(), data, null);
    }

    /**
     * 创建无数据成功响应。
     *
     * @param <T> 数据类型
     * @return 成功响应对象
     */
    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMessage(), null, null);
    }

    /**
     * 根据错误码创建失败响应。
     *
     * @param errorCode 错误码枚举
     * @param <T>       数据类型
     * @return 失败响应对象
     */
    public static <T> ApiResponse<T> failure(ErrorCode errorCode) {
        return failure(errorCode, errorCode.getMessage());
    }

    /**
     * 根据错误码与自定义消息创建失败响应。
     *
     * @param errorCode 错误码枚举
     * @param message   自定义错误信息
     * @param <T>       数据类型
     * @return 失败响应对象
     */
    public static <T> ApiResponse<T> failure(ErrorCode errorCode, String message) {
        ApiResponse<T> response = new ApiResponse<>(errorCode.getCode(), message, null, errorCode.getCode());
        response.setHttpStatus(errorCode.getHttpStatus());
        return response;
    }

    /**
     * 根据错误码创建失败响应（语义与 {@link #failure(ErrorCode)} 一致，保留旧方法名兼容）。
     *
     * @param errorCode 错误码枚举
     * @param <T>       数据类型
     * @return 失败响应对象
     */
    public static <T> ApiResponse<T> error(ErrorCode errorCode) {
        return failure(errorCode);
    }

    /**
     * 根据错误码与自定义消息创建失败响应（语义与 {@link #failure(ErrorCode, String)} 一致）。
     *
     * @param errorCode 错误码枚举
     * @param message   自定义错误信息
     * @param <T>       数据类型
     * @return 失败响应对象
     */
    public static <T> ApiResponse<T> error(ErrorCode errorCode, String message) {
        return failure(errorCode, message);
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
     * 判断是否成功。
     *
     * @return true 表示成功
     */
    @JsonIgnore
    public boolean isSuccess() {
        return ErrorCode.SUCCESS.getCode().equals(this.code);
    }
}
