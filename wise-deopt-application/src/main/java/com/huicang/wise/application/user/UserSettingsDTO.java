package com.huicang.wise.application.user;

import java.util.Map;

/**
 * 类功能描述：用户设置DTO
 *
 * @author xingchentye
 * @version 0.1.24
 * @since 2026-02-27
 */
public class UserSettingsDTO {

    /** 用户ID */
    private Long userId;

    /** 设置项键值对 */
    private Map<String, String> settings;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Map<String, String> getSettings() {
        return settings;
    }

    public void setSettings(Map<String, String> settings) {
        this.settings = settings;
    }
}
