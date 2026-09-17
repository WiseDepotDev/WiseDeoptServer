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

    @Column(name = "nfc_uid")
    private String nfcUid;

    @Column(name = "rfid")
    private String rfid;

    @Column(name = "pin_salt", columnDefinition = "char(16)")
    private String pinSalt;

    @Column(name = "pin_hash", columnDefinition = "char(64)")
    private String pinHash;

    @Column(name = "status", columnDefinition = "tinyint unsigned")
    private Short status;

    @Column(name = "status_reason")
    private String statusReason;

    @Column(name = "last_success_use_time")
    private LocalDateTime lastSuccessUseTime;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "create_by")
    private Long createBy;

    @Column(name = "update_time")
    private LocalDateTime updateTime;

    @Column(name = "update_by")
    private Long updateBy;

    public Long getBadgeId() { return badgeId; }
    public void setBadgeId(Long badgeId) { this.badgeId = badgeId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getNfcUid() { return nfcUid; }
    public void setNfcUid(String nfcUid) { this.nfcUid = nfcUid; }
    public String getRfid() { return rfid; }
    public void setRfid(String rfid) { this.rfid = rfid; }
    public String getPinSalt() { return pinSalt; }
    public void setPinSalt(String pinSalt) { this.pinSalt = pinSalt; }
    public String getPinHash() { return pinHash; }
    public void setPinHash(String pinHash) { this.pinHash = pinHash; }
    public Short getStatus() { return status; }
    public void setStatus(Short status) { this.status = status; }
    public String getStatusReason() { return statusReason; }
    public void setStatusReason(String statusReason) { this.statusReason = statusReason; }
    public LocalDateTime getLastSuccessUseTime() { return lastSuccessUseTime; }
    public void setLastSuccessUseTime(LocalDateTime lastSuccessUseTime) { this.lastSuccessUseTime = lastSuccessUseTime; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public Long getCreateBy() { return createBy; }
    public void setCreateBy(Long createBy) { this.createBy = createBy; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
    public Long getUpdateBy() { return updateBy; }
    public void setUpdateBy(Long updateBy) { this.updateBy = updateBy; }
}
