package com.huicang.wise.infrastructure.repository.user;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_profile", indexes = {
    @Index(name = "uk_user_id", columnList = "user_id", unique = true),
    @Index(name = "uk_email", columnList = "email", unique = true),
    @Index(name = "idx_create_time", columnList = "create_time"),
    @Index(name = "avatar_file_id", columnList = "avatar_file_id"),
    @Index(name = "update_by", columnList = "update_by")
})
public class UserProfileJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "profile_id")
    private Long profileId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "nickname", nullable = false, length = 16)
    private String nickname;

    @Column(name = "email", nullable = false, length = 128)
    private String email;

    @Column(name = "avatar_file_id")
    private Long avatarFileId;

    @Column(name = "gender", nullable = false, columnDefinition = "tinyint unsigned")
    private Short gender;

    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;

    @Column(name = "update_by", nullable = false)
    private Long updateBy;

    public Long getProfileId() { 
        return profileId; 
    }
    
    public void setProfileId(Long profileId) { 
        this.profileId = profileId; 
    }
    
    public Long getUserId() { 
        return userId; 
    }
    
    public void setUserId(Long userId) { 
        this.userId = userId; 
    }
    
    public String getNickname() { 
        return nickname; 
    }
    
    public void setNickname(String nickname) { 
        this.nickname = nickname; 
    }
    
    public String getEmail() { 
        return email; 
    }
    
    public void setEmail(String email) { 
        this.email = email; 
    }
    
    public Long getAvatarFileId() { 
        return avatarFileId; 
    }
    
    public void setAvatarFileId(Long avatarFileId) { 
        this.avatarFileId = avatarFileId; 
    }
    
    public Short getGender() { 
        return gender; 
    }
    
    public void setGender(Short gender) { 
        this.gender = gender; 
    }
    
    public LocalDateTime getCreateTime() { 
        return createTime; 
    }
    
    public void setCreateTime(LocalDateTime createTime) { 
        this.createTime = createTime; 
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
