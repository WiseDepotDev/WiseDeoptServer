package com.huicang.wise.infrastructure.repository.auth;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_role", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_role_id", columnList = "role_id"),
    @Index(name = "uk_user_role", columnList = "user_id,role_id", unique = true),
    @Index(name = "create_by", columnList = "create_by")
})
public class UserRoleJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "role_id", nullable = false)
    private Long roleId;

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
    
    public Long getUserId() { 
        return userId; 
    }
    
    public void setUserId(Long userId) { 
        this.userId = userId; 
    }
    
    public Long getRoleId() { 
        return roleId; 
    }
    
    public void setRoleId(Long roleId) { 
        this.roleId = roleId; 
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
