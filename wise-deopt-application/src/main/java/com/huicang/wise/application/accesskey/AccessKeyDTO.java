package com.huicang.wise.application.accesskey;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public class AccessKeyDTO {
    private Long keyId;
    private Long userId;
    @NotBlank(message = "公钥标识不能为空")
    @Size(max = 32, message = "公钥标识长度不能超过32个字符")
    private String accessKey;
    private String secretKeyEnc;
    @NotNull(message = "状态不能为空")
    private Short status;
    @Size(max = 128, message = "用途说明长度不能超过128个字符")
    private String description;
    @NotNull(message = "创建者ID不能为空")
    private Long createBy;
    private LocalDateTime createTime;
    @NotNull(message = "更新者ID不能为空")
    private Long updateBy;
    private LocalDateTime updateTime;

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

    public Long getCreateBy() {
        return createBy;
    }

    public void setCreateBy(Long createBy) {
        this.createBy = createBy;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public Long getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(Long updateBy) {
        this.updateBy = updateBy;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}
