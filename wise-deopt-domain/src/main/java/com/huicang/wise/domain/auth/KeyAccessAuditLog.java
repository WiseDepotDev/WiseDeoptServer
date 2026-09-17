package com.huicang.wise.domain.auth;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * 密钥访问审计日志实体
 * 记录API密钥的访问日志，用于安全审计
 *
 * @author WiseDepot
 * @version 0.0.24
 * @since 2026-03-03
 */
@Entity
@Table(name = "key_access_audit_log", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_access_key", columnList = "access_key"),
    @Index(name = "idx_request_time", columnList = "request_time"),
    @Index(name = "idx_status_code", columnList = "status_code")
})
public class KeyAccessAuditLog {

    /**
     * 日志主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long logId;

    /**
     * 用户ID
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * 使用的AccessKey
     */
    @NotBlank(message = "AccessKey不能为空")
    @Size(max = 32, message = "AccessKey长度不能超过32个字符")
    @Column(name = "access_key", nullable = false, length = 32, columnDefinition = "char(32)")
    private String accessKey;

    /**
     * 请求路径
     */
    @NotBlank(message = "请求路径不能为空")
    @Size(max = 255, message = "请求路径长度不能超过255个字符")
    @Column(name = "request_uri", nullable = false, length = 255)
    private String requestUri;

    /**
     * 请求方法
     */
    @NotBlank(message = "请求方法不能为空")
    @Size(max = 16, message = "请求方法长度不能超过16个字符")
    @Column(name = "method", nullable = false, length = 16)
    private String method;

    /**
     * 来源IP地址
     */
    @NotBlank(message = "IP地址不能为空")
    @Size(max = 39, message = "IP地址长度不能超过39个字符")
    @Column(name = "ip_address", nullable = false, length = 39)
    private String ipAddress;

    /**
     * 响应状态码
     */
    @Column(name = "status_code", nullable = false, columnDefinition = "tinyint unsigned")
    private Short statusCode;

    /**
     * 响应消息
     */
    @Size(max = 255, message = "响应消息长度不能超过255个字符")
    @Column(name = "result_message", length = 255)
    private String resultMessage;

    /**
     * 请求耗时（毫秒）
     */
    @Column(name = "duration_ms", nullable = false)
    private Integer durationMs;

    /**
     * 请求时间
     */
    @Column(name = "request_time", nullable = false, updatable = false)
    private LocalDateTime requestTime;

    /**
     * 获取日志主键ID
     *
     * @return 日志主键ID
     */
    public Long getLogId() {
        return logId;
    }

    /**
     * 设置日志主键ID
     *
     * @param logId 日志主键ID
     */
    public void setLogId(Long logId) {
        this.logId = logId;
    }

    /**
     * 获取用户ID
     *
     * @return 用户ID
     */
    public Long getUserId() {
        return userId;
    }

    /**
     * 设置用户ID
     *
     * @param userId 用户ID
     */
    public void setUserId(Long userId) {
        this.userId = userId;
    }

    /**
     * 获取AccessKey
     *
     * @return AccessKey
     */
    public String getAccessKey() {
        return accessKey;
    }

    /**
     * 设置AccessKey
     *
     * @param accessKey AccessKey
     */
    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    /**
     * 获取请求路径
     *
     * @return 请求路径
     */
    public String getRequestUri() {
        return requestUri;
    }

    /**
     * 设置请求路径
     *
     * @param requestUri 请求路径
     */
    public void setRequestUri(String requestUri) {
        this.requestUri = requestUri;
    }

    /**
     * 获取请求方法
     *
     * @return 请求方法
     */
    public String getMethod() {
        return method;
    }

    /**
     * 设置请求方法
     *
     * @param method 请求方法
     */
    public void setMethod(String method) {
        this.method = method;
    }

    /**
     * 获取IP地址
     *
     * @return IP地址
     */
    public String getIpAddress() {
        return ipAddress;
    }

    /**
     * 设置IP地址
     *
     * @param ipAddress IP地址
     */
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    /**
     * 获取响应状态码
     *
     * @return 响应状态码
     */
    public Short getStatusCode() {
        return statusCode;
    }

    /**
     * 设置响应状态码
     *
     * @param statusCode 响应状态码
     */
    public void setStatusCode(Short statusCode) {
        this.statusCode = statusCode;
    }

    /**
     * 获取响应消息
     *
     * @return 响应消息
     */
    public String getResultMessage() {
        return resultMessage;
    }

    /**
     * 设置响应消息
     *
     * @param resultMessage 响应消息
     */
    public void setResultMessage(String resultMessage) {
        this.resultMessage = resultMessage;
    }

    /**
     * 获取请求耗时
     *
     * @return 请求耗时（毫秒）
     */
    public Integer getDurationMs() {
        return durationMs;
    }

    /**
     * 设置请求耗时
     *
     * @param durationMs 请求耗时（毫秒）
     */
    public void setDurationMs(Integer durationMs) {
        this.durationMs = durationMs;
    }

    /**
     * 获取请求时间
     *
     * @return 请求时间
     */
    public LocalDateTime getRequestTime() {
        return requestTime;
    }

    /**
     * 设置请求时间
     *
     * @param requestTime 请求时间
     */
    public void setRequestTime(LocalDateTime requestTime) {
        this.requestTime = requestTime;
    }
}
