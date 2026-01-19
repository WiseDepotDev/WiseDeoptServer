package com.huicang.wise.application.auth;

/**
 * 类功能描述：登录请求参数
 *
 * @author xingchentye
 * @date 2026-01-19
 * @modified xingchentye 2026-01-19 定义基础登录请求字段
 */
public class LoginRequest {

    /**
     * 方法功能描述：登录名
     */
    private String username;

    /**
     * 方法功能描述：登录密码
     */
    private String password;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
