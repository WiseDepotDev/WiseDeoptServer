package com.huicang.wise.domain.user;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * 用户访问密钥实体 存储用户的API访问密钥信息
 *
 * @author WiseDepot
 * @version 0.0.23
 * @since 2026-03-03
 */
public class UserAccessKey {

    /** 密钥主键ID */
    private Long keyId;

    /** 关联的用户ID */
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    /** 公钥标识 */
    @NotBlank(message = "公钥标识不能为空")
    @Size(max = 32, message = "公钥标识长度不能超过32个字符")
    private String accessKey;

    /** 私钥密文 */
    @NotBlank(message = "私钥密文不能为空")
    private String secretKeyEnc;

    /** 状态：0：禁用 1：启用 */
    @NotNull(message = "状态不能为空")
    private Short status = 1;

    /** 用途说明 */
    @Size(max = 128, message = "用途说明长度不能超过128个字符")
    private String description;

    /** 创建者ID */
    @NotNull(message = "创建者ID不能为空")
    private Long createBy;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新者ID */
    @NotNull(message = "更新者ID不能为空")
    private Long updateBy;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /**
     * 获取密钥主键ID
     *
     * @return 密钥主键ID
     */
    public Long getKeyId() {
        return keyId;
    }

    /**
     * 设置密钥主键ID
     *
     * @param keyId 密钥主键ID
     */
    public void setKeyId(Long keyId) {
        this.keyId = keyId;
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
     * 获取公钥标识
     *
     * @return 公钥标识
     */
    public String getAccessKey() {
        return accessKey;
    }

    /**
     * 设置公钥标识
     *
     * @param accessKey 公钥标识
     */
    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    /**
     * 获取私钥密文
     *
     * @return 私钥密文
     */
    public String getSecretKeyEnc() {
        return secretKeyEnc;
    }

    /**
     * 设置私钥密文
     *
     * @param secretKeyEnc 私钥密文
     */
    public void setSecretKeyEnc(String secretKeyEnc) {
        this.secretKeyEnc = secretKeyEnc;
    }

    /**
     * 获取状态
     *
     * @return 状态
     */
    public Short getStatus() {
        return status;
    }

    /**
     * 设置状态
     *
     * @param status 状态
     */
    public void setStatus(Short status) {
        this.status = status;
    }

    /**
     * 获取用途说明
     *
     * @return 用途说明
     */
    public String getDescription() {
        return description;
    }

    /**
     * 设置用途说明
     *
     * @param description 用途说明
     */
    public void setDescription(String description) {
        this.description = description;
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
}
