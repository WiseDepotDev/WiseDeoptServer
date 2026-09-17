package com.huicang.wise.infrastructure.repository.auth;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "role", indexes = {
    @Index(name = "uk_name", columnList = "name", unique = true),
    @Index(name = "idx_create_time", columnList = "create_time"),
    @Index(name = "create_by", columnList = "create_by"),
    @Index(name = "update_by", columnList = "update_by")
})
public class RoleJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "role_id")
    private Long roleId;

    @Column(name = "name", nullable = false, unique = true, length = 32)
    private String name;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    @Column(name = "create_by", nullable = false)
    private Long createBy;

    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;

    @Column(name = "update_by", nullable = false)
    private Long updateBy;

    public Long getRoleId() { 
        return roleId; 
    }
    
    public void setRoleId(Long roleId) { 
        this.roleId = roleId; 
    }
    
    public String getName() { 
        return name; 
    }
    
    public void setName(String name) { 
        this.name = name; 
    }
    
    public String getDescription() { 
        return description; 
    }
    
    public void setDescription(String description) { 
        this.description = description; 
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
    
    public LocalDateTime getUpdateTime() { 
        return updateTime; 
    }
    
    public void setUpdateTime(LocalDateTime updateTime) { 
        this.updateTime = updateTime; 
    }
    
    public Long getUpdateBy() { 
        return updateBy; 
    }
    
    public void setUpdateBy(Long updateBy) { 
        this.updateBy = updateBy; 
    }
}
