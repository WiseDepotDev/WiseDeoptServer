package com.huicang.wise.application.user;

import io.swagger.v3.oas.annotations.media.Schema;
/**
 * 类功能描述：用户更新请求
 *
 * @author xingchentye
 * @date 2026-01-22
 */
@Schema(description = "用户更新请求")
public class UserUpdateRequest {

    @Schema(description = "用户ID", hidden = true)
    private Long userId;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "头像URL")
    private String avatar;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "是否启用")
    private Boolean enabled;

    @Schema(description = "NFC ID")
    private String nfcId;

    @Schema(description = "新密码（留空则不修改）")
    private String password;

    @Schema(description = "新PIN码（留空则不修改）")
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

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
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

