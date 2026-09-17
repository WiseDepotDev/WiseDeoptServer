package com.huicang.wise.domain.auth;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * 权限实体
 * 存储系统权限信息
 *
 * @author WiseDepot
 * @version 0.0.22
 * @since 2026-02-27
 */
@Entity
@Table(name = "permission", indexes = {
    @Index(name = "uk_name", columnList = "name", unique = true),
    @Index(name = "uk_code", columnList = "code", unique = true),
    @Index(name = "idx_create_time", columnList = "create_time")
})
public class Permission {

    /**
     * 权限主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "permission_id")
    private Long permissionId;

    /**
     * 权限名称
     */
    @NotBlank(message = "权限名称不能为空")
    @Size(max = 64, message = "权限名称长度不能超过64个字符")
    @Column(name = "name", nullable = false, unique = true, length = 64)
    private String name;

    /**
     * 权限编码，唯一标识权限
     */
    @NotBlank(message = "权限编码不能为空")
    @Size(max = 128, message = "权限编码长度不能超过128个字符")
    @Column(name = "code", nullable = false, unique = true, length = 128)
    private String code;

    /**
     * 权限描述
     */
    @Size(max = 255, message = "权限描述长度不能超过255个字符")
    @Column(name = "description", length = 255)
    private String description;

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
     * 更新时间
     */
    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;

    /**
     * 更新者ID
     */
    @Column(name = "update_by", nullable = false)
    private Long updateBy;

    /**
     * 获取权限主键ID
     *
     * @return 权限主键ID
     */
    public Long getPermissionId() {
        return permissionId;
    }

    /**
     * 设置权限主键ID
     *
     * @param permissionId 权限主键ID
     */
    public void setPermissionId(Long permissionId) {
        this.permissionId = permissionId;
    }

    /**
     * 获取权限名称
     *
     * @return 权限名称
     */
    public String getName() {
        return name;
    }

    /**
     * 设置权限名称
     *
     * @param name 权限名称
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * 获取权限编码
     *
     * @return 权限编码
     */
    public String getCode() {
        return code;
    }

    /**
     * 设置权限编码
     *
     * @param code 权限编码
     */
    public void setCode(String code) {
        this.code = code;
    }

    /**
     * 获取权限描述
     *
     * @return 权限描述
     */
    public String getDescription() {
        return description;
    }

    /**
     * 设置权限描述
     *
     * @param description 权限描述
     */
    public void setDescription(String description) {
        this.description = description;
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
}
