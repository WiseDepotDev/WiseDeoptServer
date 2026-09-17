package com.huicang.wise.application.auth;

/**
 * 刷新令牌请求
 *
 * @author WiseDepot
 * @version 0.1.0
 * @since 2026-02-27
 */
public class RefreshTokenRequest {

    /**
     * 刷新令牌
     */
    private String refreshToken;

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}
