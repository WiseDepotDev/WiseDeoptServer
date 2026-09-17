package com.huicang.wise.domain.user;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * 用户个人资料实体
 * 存储用户的个人详细信息，如昵称、邮箱等
 *
 * @author WiseDepot
 * @version 0.0.23
 * @since 2026-03-03
 */
@Entity
@Table(name = "user_profile", indexes = {
    @Index(name = "uk_user_id", columnList = "user_id", unique = true),
    @Index(name = "uk_email", columnList = "email", unique = true),
    @Index(name = "idx_create_time", columnList = "create_time"),
    @Index(name = "avatar_file_id", columnList = "avatar_file_id")
})
public class UserProfile {

    /**
     * 个人资料主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "profile_id")
    private Long profileId;

    /**
     * 关联的用户ID
     */
    @NotNull(message = "用户ID不能为空")
    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    /**
     * 昵称
     */
    @NotBlank(message = "昵称不能为空")
    @Size(max = 16, message = "昵称长度不能超过16个字符")
    @Column(name = "nickname", nullable = false, length = 16)
    private String nickname;

    /**
     * 邮箱
     */
    @NotBlank(message = "邮箱不能为空")
    @Size(max = 128, message = "邮箱长度不能超过128个字符")
    @Column(name = "email", nullable = false, unique = true, length = 128)
    private String email;

    /**
     * 头像文件ID
     */
    @Column(name = "avatar_file_id")
    private Long avatarFileId;

    /**
     * 性别：0：保密 1：男 2：女
     */
    @NotNull(message = "性别不能为空")
    @Column(name = "gender", nullable = false)
    private Integer gender = 0;

    /**
     * 创建时间
     */
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

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
     * 获取个人资料主键ID
     *
     * @return 个人资料主键ID
     */
    public Long getProfileId() {
        return profileId;
    }

    /**
     * 设置个人资料主键ID
     *
     * @param profileId 个人资料主键ID
     */
    public void setProfileId(Long profileId) {
        this.profileId = profileId;
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
     * 获取昵称
     *
     * @return 昵称
     */
    public String getNickname() {
        return nickname;
    }

    /**
     * 设置昵称
     *
     * @param nickname 昵称
     */
    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    /**
     * 获取邮箱
     *
     * @return 邮箱
     */
    public String getEmail() {
        return email;
    }

    /**
     * 设置邮箱
     *
     * @param email 邮箱
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * 获取头像文件ID
     *
     * @return 头像文件ID
     */
    public Long getAvatarFileId() {
        return avatarFileId;
    }

    /**
     * 设置头像文件ID
     *
     * @param avatarFileId 头像文件ID
     */
    public void setAvatarFileId(Long avatarFileId) {
        this.avatarFileId = avatarFileId;
    }

    /**
     * 获取性别
     *
     * @return 性别
     */
    public Integer getGender() {
        return gender;
    }

    /**
     * 设置性别
     *
     * @param gender 性别
     */
    public void setGender(Integer gender) {
        this.gender = gender;
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
