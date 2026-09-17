package com.huicang.wise.domain.user;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * NFC工牌信息实体
 * 存储NFC工牌的基本信息和状态
 *
 * @author WiseDepot
 * @version 0.0.24
 * @since 2026-03-03
 */
@Entity
@Table(name = "nfc_badge", indexes = {
    @Index(name = "uk_nfc_uid", columnList = "nfc_uid", unique = true),
    @Index(name = "uk_rfid", columnList = "rfid", unique = true),
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_create_time", columnList = "create_time")
})
public class NfcBadge {

    /**
     * 工牌主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "badge_id")
    private Long badgeId;

    /**
     * 关联的用户ID
     */
    @NotNull(message = "用户ID不能为空")
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * NFC工牌芯片UID
     */
    @NotBlank(message = "NFC UID不能为空")
    @Size(max = 64, message = "NFC UID长度不能超过64个字符")
    @Column(name = "nfc_uid", nullable = false, unique = true, length = 64)
    private String nfcUid;

    /**
     * RFID芯片UID
     */
    @NotBlank(message = "RFID不能为空")
    @Size(max = 128, message = "RFID长度不能超过128个字符")
    @Column(name = "rfid", nullable = false, unique = true, length = 128)
    private String rfid;

    /**
     * PIN码盐值
     */
    @NotBlank(message = "PIN码盐值不能为空")
    @Size(max = 16, message = "PIN码盐值长度不能超过16个字符")
    @Column(name = "pin_salt", nullable = false, length = 16, columnDefinition = "char(16)")
    private String pinSalt;

    /**
     * PIN码哈希值
     */
    @NotBlank(message = "PIN码哈希值不能为空")
    @Size(max = 64, message = "PIN码哈希值长度不能超过64个字符")
    @Column(name = "pin_hash", nullable = false, length = 64, columnDefinition = "char(64)")
    private String pinHash;

    /**
     * 工牌状态：0：未激活 1：正常使用 2：挂失 3：损坏 4：停用 5：已作废
     */
    @NotNull(message = "工牌状态不能为空")
    @Column(name = "status", nullable = false, columnDefinition = "tinyint unsigned")
    private Short status = 0;

    /**
     * 状态变更原因
     */
    @Size(max = 255, message = "状态变更原因长度不能超过255个字符")
    @Column(name = "status_reason", length = 255)
    private String statusReason;

    /**
     * 最近一次成功使用时间
     */
    @Column(name = "last_success_use_time")
    private LocalDateTime lastSuccessUseTime;

    /**
     * 创建时间
     */
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    /**
     * 创建者ID
     */
    @NotNull(message = "创建者ID不能为空")
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
    @NotNull(message = "更新者ID不能为空")
    @Column(name = "update_by", nullable = false)
    private Long updateBy;

    /**
     * 获取工牌主键ID
     *
     * @return 工牌主键ID
     */
    public Long getBadgeId() {
        return badgeId;
    }

    /**
     * 设置工牌主键ID
     *
     * @param badgeId 工牌主键ID
     */
    public void setBadgeId(Long badgeId) {
        this.badgeId = badgeId;
    }

    /**
     * 获取关联的用户ID
     *
     * @return 用户ID
     */
    public Long getUserId() {
        return userId;
    }

    /**
     * 设置关联的用户ID
     *
     * @param userId 用户ID
     */
    public void setUserId(Long userId) {
        this.userId = userId;
    }

    /**
     * 获取NFC工牌芯片UID
     *
     * @return NFC工牌芯片UID
     */
    public String getNfcUid() {
        return nfcUid;
    }

    /**
     * 设置NFC工牌芯片UID
     *
     * @param nfcUid NFC工牌芯片UID
     */
    public void setNfcUid(String nfcUid) {
        this.nfcUid = nfcUid;
    }

    /**
     * 获取RFID芯片UID
     *
     * @return RFID芯片UID
     */
    public String getRfid() {
        return rfid;
    }

    /**
     * 设置RFID芯片UID
     *
     * @param rfid RFID芯片UID
     */
    public void setRfid(String rfid) {
        this.rfid = rfid;
    }

    /**
     * 获取PIN码盐值
     *
     * @return PIN码盐值
     */
    public String getPinSalt() {
        return pinSalt;
    }

    /**
     * 设置PIN码盐值
     *
     * @param pinSalt PIN码盐值
     */
    public void setPinSalt(String pinSalt) {
        this.pinSalt = pinSalt;
    }

    /**
     * 获取PIN码哈希值
     *
     * @return PIN码哈希值
     */
    public String getPinHash() {
        return pinHash;
    }

    /**
     * 设置PIN码哈希值
     *
     * @param pinHash PIN码哈希值
     */
    public void setPinHash(String pinHash) {
        this.pinHash = pinHash;
    }

    /**
     * 获取工牌状态
     *
     * @return 工牌状态
     */
    public Short getStatus() {
        return status;
    }

    /**
     * 设置工牌状态
     *
     * @param status 工牌状态
     */
    public void setStatus(Short status) {
        this.status = status;
    }

    /**
     * 获取状态变更原因
     *
     * @return 状态变更原因
     */
    public String getStatusReason() {
        return statusReason;
    }

    /**
     * 设置状态变更原因
     *
     * @param statusReason 状态变更原因
     */
    public void setStatusReason(String statusReason) {
        this.statusReason = statusReason;
    }

    /**
     * 获取最近一次成功使用时间
     *
     * @return 最近一次成功使用时间
     */
    public LocalDateTime getLastSuccessUseTime() {
        return lastSuccessUseTime;
    }

    /**
     * 设置最近一次成功使用时间
     *
     * @param lastSuccessUseTime 最近一次成功使用时间
     */
    public void setLastSuccessUseTime(LocalDateTime lastSuccessUseTime) {
        this.lastSuccessUseTime = lastSuccessUseTime;
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
