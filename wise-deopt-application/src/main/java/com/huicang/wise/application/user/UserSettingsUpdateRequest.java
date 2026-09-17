package com.huicang.wise.application.user;

import java.util.Map;

/**
 * 类功能描述：用户设置更新请求
 *
 * @author xingchentye
 * @version 0.1.24
 * @since 2026-02-27
 */
public class UserSettingsUpdateRequest {

    /**
     * 设置项键值对
     */
    private Map<String, String> settings;

    public Map<String, String> getSettings() {
        return settings;
    }

    public void setSettings(Map<String, String> settings) {
        this.settings = settings;
    }
}