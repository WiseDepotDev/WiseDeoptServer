package com.huicang.wise.domain.user;

import java.time.LocalDateTime;

/**
 * 类功能描述：用户核心信息实体
 *
 * @author xingchentye
 * @date 2026-01-19
 * @modified xingchentye 2026-01-19 定义用户核心信息字段
 */
public class UserCore {

    /**
     * 方法功能描述：用户主键ID
     */
    private Long userId;

    /**
     * 方法功能描述：登录名
     */
    private String username;

    /**
     * 方法功能描述：邮箱地址
     */
    private String email;

    /**
     * 方法功能描述：密码摘要
     */
    private String passwordHash;

    /**
     * 方法功能描述：是否启用
     */
    private Boolean enabled;

    /**
     * 方法功能描述：创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 方法功能描述：最后更新时间
     */
    private LocalDateTime updatedAt;

    private String nfcId;

    private String pinHash;

    private Integer loginFailCount;

    private LocalDateTime lockedUntil;

    /**
     * 方法功能描述：获取用户主键ID
     *
     * @return 用户主键ID
     */
    public Long getUserId() {
        return userId;
    }

    /**
     * 方法功能描述：设置用户主键ID
     *
     * @param userId 用户主键ID
     * @return 无
     */
    public void setUserId(Long userId) {
        this.userId = userId;
    }

    /**
     * 方法功能描述：获取登录名
     *
     * @return 登录名
     */
    public String getUsername() {
        return username;
    }

    /**
     * 方法功能描述：设置登录名
     *
     * @param username 登录名
     * @return 无
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * 方法功能描述：获取邮箱地址
     *
     * @return 邮箱地址
     */
    public String getEmail() {
        return email;
    }

    /**
     * 方法功能描述：设置邮箱地址
     *
     * @param email 邮箱地址
     * @return 无
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * 方法功能描述：获取密码摘要
     *
     * @return 密码摘要
     */
    public String getPasswordHash() {
        return passwordHash;
    }

    /**
     * 方法功能描述：设置密码摘要
     *
     * @param passwordHash 密码摘要
     * @return 无
     */
    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getNfcId() {
        return nfcId;
    }

    public void setNfcId(String nfcId) {
        this.nfcId = nfcId;
    }

    public String getPinHash() {
        return pinHash;
    }

    public void setPinHash(String pinHash) {
        this.pinHash = pinHash;
    }

    public Integer getLoginFailCount() {
        return loginFailCount;
    }

    public void setLoginFailCount(Integer loginFailCount) {
        this.loginFailCount = loginFailCount;
    }

    public LocalDateTime getLockedUntil() {
        return lockedUntil;
    }

    public void setLockedUntil(LocalDateTime lockedUntil) {
        this.lockedUntil = lockedUntil;
    }

    /**
     * 方法功能描述：获取是否启用
     *
     * @return 是否启用
     */
    public Boolean getEnabled() {
        return enabled;
    }

    /**
     * 方法功能描述：设置是否启用
     *
     * @param enabled 是否启用
     * @return 无
     */
    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * 方法功能描述：获取创建时间
     *
     * @return 创建时间
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * 方法功能描述：设置创建时间
     *
     * @param createdAt 创建时间
     * @return 无
     */
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * 方法功能描述：获取最后更新时间
     *
     * @return 最后更新时间
     */
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /**
     * 方法功能描述：设置最后更新时间
     *
     * @param updatedAt 最后更新时间
     * @return 无
     */
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
