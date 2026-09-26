package com.huicang.wise.domain.user;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * 用户核心信息实体 存储用户的基本账户信息
 *
 * @author WiseDepot
 * @version 0.0.23
 * @since 2026-03-03
 */
public class UserCore {

    /** 用户主键ID */
    private Long userId;

    /** 用户名 */
    @NotBlank(message = "用户名不能为空")
    @Size(max = 16, message = "用户名长度不能超过16个字符")
    private String username;

    /** 用户类型：0：人工用户 1：设备用户 */
    @NotNull(message = "用户类型不能为空")
    private Short userType;

    /** 所属设备id */
    private Long ownerDeviceId;

    /** 用户状态：0：封禁 1：正常 */
    @NotNull(message = "用户状态不能为空")
    private Short status = 1;

    /** 是否删除：0：未删除 1：已删除 */
    @NotNull(message = "是否删除不能为空")
    private Short isDeleted = 0;

    /** 创建者ID */
    @NotNull(message = "创建者ID不能为空")
    private Long createBy;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新者ID */
    @NotNull(message = "更新者ID不能为空")
    private Long updateBy;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /**
     * 获取用户主键ID
     *
     * @return 用户主键ID
     */
    public Long getUserId() {
        return userId;
    }

    /**
     * 设置用户主键ID
     *
     * @param userId 用户主键ID
     */
    public void setUserId(Long userId) {
        this.userId = userId;
    }

    /**
     * 获取用户名
     *
     * @return 用户名
     */
    public String getUsername() {
        return username;
    }

    /**
     * 设置用户名
     *
     * @param username 用户名
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * 获取用户类型
     *
     * @return 用户类型
     */
    public Short getUserType() {
        return userType;
    }

    /**
     * 设置用户类型
     *
     * @param userType 用户类型
     */
    public void setUserType(Short userType) {
        this.userType = userType;
    }

    /**
     * 获取所属设备id
     *
     * @return 所属设备id
     */
    public Long getOwnerDeviceId() {
        return ownerDeviceId;
    }

    /**
     * 设置所属设备id
     *
     * @param ownerDeviceId 所属设备id
     */
    public void setOwnerDeviceId(Long ownerDeviceId) {
        this.ownerDeviceId = ownerDeviceId;
    }

    /**
     * 获取用户状态
     *
     * @return 用户状态
     */
    public Short getStatus() {
        return status;
    }

    /**
     * 设置用户状态
     *
     * @param status 用户状态
     */
    public void setStatus(Short status) {
        this.status = status;
    }

    /**
     * 获取是否删除
     *
     * @return 是否删除
     */
    public Short getIsDeleted() {
        return isDeleted;
    }

    /**
     * 设置是否删除
     *
     * @param isDeleted 是否删除
     */
    public void setIsDeleted(Short isDeleted) {
        this.isDeleted = isDeleted;
    }

    /**
     * 获取创建者ID
     *
     * @return 创建者ID
     */
    public Long getCreateBy() {
        return createBy;
    }

    /**
     * 设置创建者ID
     *
     * @param createBy 创建者ID
     */
    public void setCreateBy(Long createBy) {
        this.createBy = createBy;
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
     * 获取更新者ID
     *
     * @return 更新者ID
     */
    public Long getUpdateBy() {
        return updateBy;
    }

    /**
     * 设置更新者ID
     *
     * @param updateBy 更新者ID
     */
    public void setUpdateBy(Long updateBy) {
        this.updateBy = updateBy;
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
