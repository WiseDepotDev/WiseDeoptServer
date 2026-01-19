package com.huicang.wise.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 类功能描述：MinIO配置属性
 *
 * @author xingchentye
 * @date 2026-01-19
 * @modified xingchentye 2026-01-19 定义MinIO配置项
 */
@ConfigurationProperties(prefix = "wise.minio")
public class MinioProperties {

    /**
     * 方法功能描述：MinIO服务地址
     */
    private String endpoint;

    /**
     * 方法功能描述：访问密钥
     */
    private String accessKey;

    /**
     * 方法功能描述：访问密钥密码
     */
    private String secretKey;

    /**
     * 方法功能描述：是否使用HTTPS
     */
    private boolean secure;

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public boolean isSecure() {
        return secure;
    }

    public void setSecure(boolean secure) {
        this.secure = secure;
    }
}

