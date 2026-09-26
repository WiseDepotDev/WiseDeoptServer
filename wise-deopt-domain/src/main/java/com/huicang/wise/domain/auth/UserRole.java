package com.huicang.wise.domain.auth;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 用户角色关联实体 存储用户与角色的关联关系
 *
 * @author WiseDepot
 * @version 0.0.23
 * @since 2026-03-03
 */
public class UserRole {

    /** 关联主键ID */
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 角色ID */
    private Long roleId;

    /** 创建者ID */
    private Long createBy;

    /** 创建时间 */
    private LocalDateTime createTime;

    /**
     * 获取关联主键ID
     *
     * @return 关联主键ID
     */
    public Long getId() {
        return id;
    }

    /**
     * 设置关联主键ID
     *
     * @param id 关联主键ID
     */
    public void setId(Long id) {
        this.id = id;
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
     * 获取角色ID
     *
     * @return 角色ID
     */
    public Long getRoleId() {
        return roleId;
    }

    /**
     * 设置角色ID
     *
     * @param roleId 角色ID
     */
    public void setRoleId(Long roleId) {
        this.roleId = roleId;
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
}
