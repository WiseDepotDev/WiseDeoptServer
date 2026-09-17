package com.huicang.wise.infrastructure.repository.user;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_access_key", indexes = {
    @Index(name = "uk_access_key", columnList = "access_key", unique = true),
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_create_time", columnList = "create_time")
})
public class UserAccessKeyJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "key_id")
    private Long keyId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "access_key", nullable = false, unique = true, length = 32, columnDefinition = "char(32)")
    private String accessKey;

    @Column(name = "secret_key_enc", nullable = false, columnDefinition = "text")
    private String secretKeyEnc;

    @Column(name = "status", nullable = false, columnDefinition = "tinyint unsigned")
    private Short status = 1;

    @Column(name = "description", length = 128)
    private String description;

    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    @Column(name = "create_by", nullable = false)
    private Long createBy;

    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;

    @Column(name = "update_by", nullable = false)
    private Long updateBy;

    public Long getKeyId() {
        return keyId;
    }

    public void setKeyId(Long keyId) {
        this.keyId = keyId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getSecretKeyEnc() {
        return secretKeyEnc;
    }

    public void setSecretKeyEnc(String secretKeyEnc) {
        this.secretKeyEnc = secretKeyEnc;
    }

    public Short getStatus() {
        return status;
    }

    public void setStatus(Short status) {
        this.status = status;
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
