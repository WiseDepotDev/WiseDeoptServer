package com.huicang.wise.domain.auth;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * 用户登录日志实体 记录用户的登录历史，用于安全审计
 *
 * @author WiseDepot
 * @version 0.0.23
 * @since 2026-03-03
 */
public class UserLoginLog {

    /** 日志主键ID */
    private Long logId;

    /** 用户ID */
    private Long userId;

    /** 登录IP */
    @NotBlank(message = "登录IP不能为空")
    @Size(max = 39, message = "登录IP长度不能超过39个字符")
    private String ipAddress;

    /** 用户代理 */
    @NotBlank(message = "用户代理不能为空")
    private String userAgent;

    /** 登录方式：0：用户名+密码 1：邮箱+密码 2：邮箱+验证码 3：NFC工牌+PIN码 */
    @NotNull(message = "登录方式不能为空")
    private Short loginType;

    /** 登录结果：0：失败 1：成功 */
    @NotNull(message = "登录结果不能为空")
    private Short success;

    /** 登录失败原因 */
    @Size(max = 255, message = "登录失败原因长度不能超过255个字符")
    private String failureReason;

    /** 登录时间 */
    @NotNull(message = "登录时间不能为空")
    private LocalDateTime loginTime;

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
     * 获取登录IP
     *
     * @return 登录IP
     */
    public String getIpAddress() {
        return ipAddress;
    }

    /**
     * 设置登录IP
     *
     * @param ipAddress 登录IP
     */
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    /**
     * 获取用户代理
     *
     * @return 用户代理
     */
    public String getUserAgent() {
        return userAgent;
    }

    /**
     * 设置用户代理
     *
     * @param userAgent 用户代理
     */
    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    /**
     * 获取登录方式
     *
     * @return 登录方式
     */
    public Short getLoginType() {
        return loginType;
    }

    /**
     * 设置登录方式
     *
     * @param loginType 登录方式
     */
    public void setLoginType(Short loginType) {
        this.loginType = loginType;
    }

    /**
     * 获取登录结果
     *
     * @return 登录结果
     */
    public Short getSuccess() {
        return success;
    }

    /**
     * 设置登录结果
     *
     * @param success 登录结果
     */
    public void setSuccess(Short success) {
        this.success = success;
    }

    /**
     * 获取登录失败原因
     *
     * @return 登录失败原因
     */
    public String getFailureReason() {
        return failureReason;
    }

    /**
     * 设置登录失败原因
     *
     * @param failureReason 登录失败原因
     */
    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    /**
     * 获取登录时间
     *
     * @return 登录时间
     */
    public LocalDateTime getLoginTime() {
        return loginTime;
    }

    /**
     * 设置登录时间
     *
     * @param loginTime 登录时间
     */
    public void setLoginTime(LocalDateTime loginTime) {
        this.loginTime = loginTime;
    }
}
