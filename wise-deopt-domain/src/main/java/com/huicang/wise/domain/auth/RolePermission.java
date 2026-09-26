package com.huicang.wise.domain.auth;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 角色权限关联实体 存储角色与权限的关联关系
 *
 * @author WiseDepot
 * @version 0.0.22
 * @since 2026-02-27
 */
public class RolePermission {

    /** 关联主键ID */
    private Long id;

    /** 角色ID */
    private Long roleId;

    /** 权限ID */
    private Long permissionId;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 创建者ID */
    private Long createBy;

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
     * 获取权限ID
     *
     * @return 权限ID
     */
    public Long getPermissionId() {
        return permissionId;
    }

    /**
     * 设置权限ID
     *
     * @param permissionId 权限ID
     */
    public void setPermissionId(Long permissionId) {
        this.permissionId = permissionId;
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
}
