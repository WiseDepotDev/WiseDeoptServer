package com.huicang.wise.domain.auth;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 角色权限关联实体
 * 存储角色与权限的关联关系
 *
 * @author WiseDepot
 * @version 0.0.22
 * @since 2026-02-27
 */
@Entity
@Table(name = "role_permission", indexes = {
    @Index(name = "idx_role_permission_role_id", columnList = "role_id"),
    @Index(name = "idx_role_permission_permission_id", columnList = "permission_id"),
    @Index(name = "uk_role_permission", columnList = "role_id,permission_id", unique = true)
})
public class RolePermission {

    /**
     * 关联主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * 角色ID
     */
    @Column(name = "role_id", nullable = false)
    private Long roleId;

    /**
     * 权限ID
     */
    @Column(name = "permission_id", nullable = false)
    private Long permissionId;

    /**
     * 创建时间
     */
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    /**
     * 创建者ID
     */
    @Column(name = "create_by", nullable = false)
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
