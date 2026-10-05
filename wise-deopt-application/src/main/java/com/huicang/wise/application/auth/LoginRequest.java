package com.huicang.wise.application.auth;

/**
 * 类功能描述：登录请求参数
 *
 * @author xingchentye
 * @date 2026-01-19
 * @modified xingchentye 2026-01-19 定义基础登录请求字段
 */
public class LoginRequest {

    /** 方法功能描述：登录名 */
    private String username;

    /** 方法功能描述：登录密码 */
    private String password;

    /**
     * 方法功能描述：人机验证票据（一次性）。
     *
     * <p><b>图形验证码已删除</b>：登录不再要求人机输入，改为"点一下按钮"由客户端完成验证， 服务端只认这张票据。缺失即失败 —— 绝不允许写成"没传就跳过"
     * （图形验证码时代在这里漏过一次，见 {@code deploy/captcha_fix_verify.py}）。
     */
    private String humanToken;

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

    public String getHumanToken() {
        return humanToken;
    }

    public void setHumanToken(String humanToken) {
        this.humanToken = humanToken;
    }
}
