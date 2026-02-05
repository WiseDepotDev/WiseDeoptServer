package com.huicang.wise.application.user;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 类功能描述：用户更新请求
 *
 * @author xingchentye
 * @date 2026-01-22
 */
@ApiModel(description = "用户更新请求")
public class UserUpdateRequest {

    @ApiModelProperty(value = "用户ID", hidden = true)
    private Long userId;

    @ApiModelProperty(value = "邮箱")
    private String email;

    @ApiModelProperty(value = "是否启用")
    private Boolean enabled;

    @ApiModelProperty(value = "NFC ID")
    private String nfcId;

    @ApiModelProperty(value = "新密码（留空则不修改）")
    private String password;

    @ApiModelProperty(value = "新PIN码（留空则不修改）")
    private String pin;

    /**
     * 角色
     */
    private String role;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String getNfcId() {
        return nfcId;
    }

    public void setNfcId(String nfcId) {
        this.nfcId = nfcId;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPin() {
        return pin;
    }

    public void setPin(String pin) {
        this.pin = pin;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
