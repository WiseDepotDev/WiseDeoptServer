package com.huicang.wise.infrastructure.repository.auth;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 用户登录日志JPA实体
 *
 * @author WiseDepot
 * @version 0.1.0
 * @since 2026-02-27
 */
@Entity
@Table(name = "user_login_log", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_login_time", columnList = "login_time"),
    @Index(name = "idx_success", columnList = "success"),
    @Index(name = "idx_login_type", columnList = "login_type"),
    @Index(name = "idx_ip_address", columnList = "ip_address")
})
public class UserLoginLogJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long logId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "ip_address", nullable = false, length = 39)
    private String ipAddress;

    @Column(name = "user_agent", nullable = false, columnDefinition = "text")
    private String userAgent;

    @Column(name = "login_type", nullable = false, columnDefinition = "tinyint unsigned")
    private Short loginType;

    @Column(name = "success", nullable = false, columnDefinition = "tinyint unsigned")
    private Short success;

    @Column(name = "failure_reason", length = 255)
    private String failureReason;

    @Column(name = "login_time", nullable = false)
    private LocalDateTime loginTime;

    public Long getLogId() {
        return logId;
    }

    public void setLogId(Long logId) {
        this.logId = logId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public Short getLoginType() {
        return loginType;
    }

    public void setLoginType(Short loginType) {
        this.loginType = loginType;
    }

    public Short getSuccess() {
        return success;
    }

    public void setSuccess(Short success) {
        this.success = success;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public LocalDateTime getLoginTime() {
        return loginTime;
    }

    public void setLoginTime(LocalDateTime loginTime) {
        this.loginTime = loginTime;
    }
}
