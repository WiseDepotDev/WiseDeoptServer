package com.huicang.wise.infrastructure.repository.user;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 类功能描述：用户核心信息JPA实体
 *
 * @author xingchentye
 * @date 2026-01-19
 * @modified xingchentye 2026-01-19 映射user_core表
 */
@Entity
@Table(name = "user_core")
public class UserCoreJpaEntity {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "username")
    private String username;

    @Column(name = "user_type", columnDefinition = "tinyint unsigned")
    private Short userType;

    @Column(name = "owner_device_id")
    private Long ownerDeviceId;

    @Column(name = "status", columnDefinition = "tinyint unsigned")
    private Short status;

    @Column(name = "is_deleted", columnDefinition = "tinyint unsigned")
    private Short isDeleted;

    @Column(name = "create_by")
    private Long createBy;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "update_by")
    private Long updateBy;

    @Column(name = "update_time")
    private LocalDateTime updateTime;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Short getUserType() {
        return userType;
    }

    public void setUserType(Short userType) {
        this.userType = userType;
    }

    public Long getOwnerDeviceId() {
        return ownerDeviceId;
    }

    public void setOwnerDeviceId(Long ownerDeviceId) {
        this.ownerDeviceId = ownerDeviceId;
    }

    public Short getStatus() {
        return status;
    }

    public void setStatus(Short status) {
        this.status = status;
    }

    public Short getIsDeleted() {
        return isDeleted;
    }

    public void setIsDeleted(Short isDeleted) {
        this.isDeleted = isDeleted;
    }

    public Long getCreateBy() {
        return createBy;
    }

    public void setCreateBy(Long createBy) {
        this.createBy = createBy;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public Long getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(Long updateBy) {
        this.updateBy = updateBy;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}

