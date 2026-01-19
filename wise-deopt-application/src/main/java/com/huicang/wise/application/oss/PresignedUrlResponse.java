package com.huicang.wise.application.oss;

/**
 * 类功能描述：预签名链接响应对象
 *
 * @author xingchentye
 * @date 2026-01-19
 * @modified xingchentye 2026-01-19 定义预签名链接字段
 */
public class PresignedUrlResponse {

    /**
     * 方法功能描述：预签名链接
     */
    private String url;

    /**
     * 方法功能描述：有效期（秒）
     */
    private Integer expiresIn;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Integer getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(Integer expiresIn) {
        this.expiresIn = expiresIn;
    }
}

