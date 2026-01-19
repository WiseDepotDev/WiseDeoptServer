package com.huicang.wise.application.auth;

/**
 * 类功能描述：登录响应结果
 *
 * @author xingchentye
 * @date 2026-01-19
 * @modified xingchentye 2026-01-19 定义基础登录响应字段
 */
public class LoginResponse {

    /**
     * 方法功能描述：访问令牌
     */
    private String accessToken;

    /**
     * 方法功能描述：刷新令牌
     */
    private String refreshToken;

    /**
     * 方法功能描述：用户名
     */
    private String username;

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}

