package com.huicang.wise.infrastructure.repository.user;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * NFC工牌信息表实体
 */
@Entity
@Table(name = "nfc_badge")
public class NfcBadgeJpaEntity {
    @Id
    @Column(name = "badge_id")
    private Long badgeId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "nfc_code")
    private String nfcCode;

    @Column(name = "status")
    private String status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Getters and Setters
    public Long getBadgeId() { return badgeId; }
    public void setBadgeId(Long badgeId) { this.badgeId = badgeId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getNfcCode() { return nfcCode; }
    public void setNfcCode(String nfcCode) { this.nfcCode = nfcCode; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
