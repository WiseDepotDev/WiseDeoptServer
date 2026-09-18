package com.huicang.wise.application.user;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 类功能描述：个人资料更新请求
 *
 * @author WiseDepot
 * @version 0.0.24
 * @since 2026-03-03
 */
public class UserProfileUpdateRequest {

    /** 昵称 */
    @Size(max = 16, message = "昵称长度不能超过16个字符")
    private String nickname;

    /** 邮箱 */
    @Pattern(regexp = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$", message = "邮箱格式不正确")
    private String email;

    /** 性别：0：保密 1：男 2：女 */
    private Integer gender;

    /** 头像文件ID */
    private Long avatarFileId;

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

    public Integer getGender() {
        return gender;
    }

    public void setGender(Integer gender) {
        this.gender = gender;
    }

    public Long getAvatarFileId() {
        return avatarFileId;
    }

    public void setAvatarFileId(Long avatarFileId) {
        this.avatarFileId = avatarFileId;
    }
}
