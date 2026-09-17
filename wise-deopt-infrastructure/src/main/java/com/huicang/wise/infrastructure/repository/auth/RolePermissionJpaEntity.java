package com.huicang.wise.infrastructure.repository.auth;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "role_permission", indexes = {
    @Index(name = "idx_role_id", columnList = "role_id"),
    @Index(name = "idx_permission_id", columnList = "permission_id"),
    @Index(name = "uk_role_permission", columnList = "role_id,permission_id", unique = true),
    @Index(name = "create_by", columnList = "create_by")
})
public class RolePermissionJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "role_id", nullable = false)
    private Long roleId;

    @Column(name = "permission_id", nullable = false)
    private Long permissionId;

    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    @Column(name = "create_by", nullable = false)
    private Long createBy;

    public Long getId() { 
        return id; 
    }
    
    public void setId(Long id) { 
        this.id = id; 
    }
    
    public Long getRoleId() { 
        return roleId; 
    }
    
    public void setRoleId(Long roleId) { 
        this.roleId = roleId; 
    }
    
    public Long getPermissionId() { 
        return permissionId; 
    }
    
    public void setPermissionId(Long permissionId) { 
        this.permissionId = permissionId; 
    }
    
    public LocalDateTime getCreateTime() { 
        return createTime; 
    }
    
    public void setCreateTime(LocalDateTime createTime) { 
        this.createTime = createTime; 
    }
    
    public Long getCreateBy() { 
        return createBy; 
    }
    
    public void setCreateBy(Long createBy) { 
        this.createBy = createBy; 
    }
}
