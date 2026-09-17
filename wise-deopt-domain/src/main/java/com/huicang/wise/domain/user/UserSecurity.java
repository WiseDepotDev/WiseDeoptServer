package com.huicang.wise.domain.user;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * 用户安全信息实体
 * 存储用户的安全相关设置，如密码哈希、盐值等
 *
 * @author WiseDepot
 * @version 0.0.24
 * @since 2026-03-03
 */
@Entity
@Table(name = "user_security", indexes = {
    @Index(name = "uk_user_id", columnList = "user_id", unique = true),
    @Index(name = "uk_active_badge_id", columnList = "active_badge_id", unique = true),
    @Index(name = "idx_last_login", columnList = "last_login_at")
})
public class UserSecurity {

    /**
     * 安全信息主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "security_id")
    private Long securityId;

    /**
     * 关联的用户ID
     */
    @NotNull(message = "用户ID不能为空")
    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    /**
     * 密码盐值
     */
    @NotBlank(message = "密码盐值不能为空")
    @Size(max = 32, message = "密码盐值长度不能超过32个字符")
    @Column(name = "salt", nullable = false, length = 32, columnDefinition = "char(32)")
    private String salt;

    /**
     * 密码哈希值
     */
    @NotBlank(message = "密码哈希值不能为空")
    @Size(max = 128, message = "密码哈希值长度不能超过128个字符")
    @Column(name = "password_hash", nullable = false, length = 128, columnDefinition = "char(128)")
    private String passwordHash;

    /**
     * 最后修改密码时间
     */
    @Column(name = "last_password_change_at")
    private LocalDateTime lastPasswordChangeAt;

    /**
     * 最后登录时间
     */
    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    /**
     * 最后登录IP
     */
    @Size(max = 39, message = "最后登录IP长度不能超过39个字符")
    @Column(name = "last_login_ip", length = 39)
    private String lastLoginIp;

    /**
     * 可用工牌ID
     */
    @Column(name = "active_badge_id", unique = true)
    private Long activeBadgeId;

    /**
     * 创建时间
     */
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;

    /**
     * 获取安全信息主键ID
     *
     * @return 安全信息主键ID
     */
    public Long getSecurityId() {
        return securityId;
    }

    /**
     * 设置安全信息主键ID
     *
     * @param securityId 安全信息主键ID
     */
    public void setSecurityId(Long securityId) {
        this.securityId = securityId;
    }

    /**
     * 获取关联的用户ID
     *
     * @return 用户ID
     */
    public Long getUserId() {
        return userId;
    }

    /**
     * 设置关联的用户ID
     *
     * @param userId 用户ID
     */
    public void setUserId(Long userId) {
        this.userId = userId;
    }

    /**
     * 获取密码盐值
     *
     * @return 密码盐值
     */
    public String getSalt() {
        return salt;
    }

    /**
     * 设置密码盐值
     *
     * @param salt 密码盐值
     */
    public void setSalt(String salt) {
        this.salt = salt;
    }

    /**
     * 获取密码哈希值
     *
     * @return 密码哈希值
     */
    public String getPasswordHash() {
        return passwordHash;
    }

    /**
     * 设置密码哈希值
     *
     * @param passwordHash 密码哈希值
     */
    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    /**
     * 获取最后修改密码时间
     *
     * @return 最后修改密码时间
     */
    public LocalDateTime getLastPasswordChangeAt() {
        return lastPasswordChangeAt;
    }

    /**
     * 设置最后修改密码时间
     *
     * @param lastPasswordChangeAt 最后修改密码时间
     */
    public void setLastPasswordChangeAt(LocalDateTime lastPasswordChangeAt) {
        this.lastPasswordChangeAt = lastPasswordChangeAt;
    }

    /**
     * 获取最后登录时间
     *
     * @return 最后登录时间
     */
    public LocalDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    /**
     * 设置最后登录时间
     *
     * @param lastLoginAt 最后登录时间
     */
    public void setLastLoginAt(LocalDateTime lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    /**
     * 获取最后登录IP
     *
     * @return 最后登录IP
     */
    public String getLastLoginIp() {
        return lastLoginIp;
    }

    /**
     * 设置最后登录IP
     *
     * @param lastLoginIp 最后登录IP
     */
    public void setLastLoginIp(String lastLoginIp) {
        this.lastLoginIp = lastLoginIp;
    }

    /**
     * 获取可用工牌ID
     *
     * @return 可用工牌ID
     */
    public Long getActiveBadgeId() {
        return activeBadgeId;
    }

    /**
     * 设置可用工牌ID
     *
     * @param activeBadgeId 可用工牌ID
     */
    public void setActiveBadgeId(Long activeBadgeId) {
        this.activeBadgeId = activeBadgeId;
    }

    /**
     * 获取创建时间
     *
     * @return 创建时间
     */
    public LocalDateTime getCreateTime() {
        return createTime;
    }

    /**
     * 设置创建时间
     *
     * @param createTime 创建时间
     */
    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    /**
     * 获取更新时间
     *
     * @return 更新时间
     */
    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    /**
     * 设置更新时间
     *
     * @param updateTime 更新时间
     */
    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}
