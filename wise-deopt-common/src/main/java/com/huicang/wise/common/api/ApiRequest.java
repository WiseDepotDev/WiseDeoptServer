package com.huicang.wise.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;

/**
 * 统一API请求基类
 *
 * @author WiseDepot
 * @version 0.0.29
 * @since 2026-02-27
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiRequest<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 请求ID
     */
    private String requestId;

    /**
     * 请求体数据
     */
    @NotNull(message = "请求体不能为空")
    private T body;

    /**
     * 时间戳
     */
    private Long timestamp;

    /**
     * 无参构造函数
     */
    public ApiRequest() {
    }

    /**
     * 全参构造函数
     *
     * @param requestId 请求ID
     * @param body      请求体数据
     * @param timestamp 时间戳
     */
    public ApiRequest(String requestId, T body, Long timestamp) {
        this.requestId = requestId;
        this.body = body;
        this.timestamp = timestamp;
    }

    /**
     * 创建请求对象
     *
     * @param body 请求体数据
     * @return 请求对象
     */
    public static <T> ApiRequest<T> of(T body) {
        return new ApiRequest<>(null, body, System.currentTimeMillis());
    }

    /**
     * 创建请求对象（带请求ID）
     *
     * @param requestId 请求ID
     * @param body      请求体数据
     * @return 请求对象
     */
    public static <T> ApiRequest<T> of(String requestId, T body) {
        return new ApiRequest<>(requestId, body, System.currentTimeMillis());
    }

    /**
     * 创建请求对象（带请求ID和时间戳）
     *
     * @param requestId 请求ID
     * @param body      请求体数据
     * @param timestamp 时间戳
     * @return 请求对象
     */
    public static <T> ApiRequest<T> of(String requestId, T body, Long timestamp) {
        return new ApiRequest<>(requestId, body, timestamp);
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public T getBody() {
        return body;
    }

    public void setBody(T body) {
        this.body = body;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }
}
