package com.huicang.wise.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * MinIO配置属性
 *
 * @author WiseDepot
 * @version 0.0.27
 * @since 2026-02-27
 */
@ConfigurationProperties(prefix = "spring.minio")
public class MinioProperties {

    /**
     * MinIO服务地址
     */
    private String endpoint;

    /**
     * 访问密钥
     */
    private String accessKey;

    /**
     * 访问密钥密码
     */
    private String secretKey;

    /**
     * 是否使用HTTPS
     */
    private boolean secure = false;

    /**
     * 默认存储桶名称
     */
    private String bucketName = "wise-depot";

    /**
     * 临时访问链接过期时间（秒）
     */
    private int presignedUrlExpiry = 3600;

    /**
     * 文件上传大小限制（字节）
     */
    private long maxFileSize = 100 * 1024 * 1024;

    /**
     * 错误日志存储桶名称
     */
    private String errorLogBucket = "wise-depot-error-logs";

    /**
     * 设备日志存储桶名称
     */
    private String deviceLogBucket = "wise-depot-device-logs";

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

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public int getPresignedUrlExpiry() {
        return presignedUrlExpiry;
    }

    public void setPresignedUrlExpiry(int presignedUrlExpiry) {
        this.presignedUrlExpiry = presignedUrlExpiry;
    }

    public long getMaxFileSize() {
        return maxFileSize;
    }

    public void setMaxFileSize(long maxFileSize) {
        this.maxFileSize = maxFileSize;
    }

    public String getErrorLogBucket() {
        return errorLogBucket;
    }

    public void setErrorLogBucket(String errorLogBucket) {
        this.errorLogBucket = errorLogBucket;
    }

    public String getDeviceLogBucket() {
        return deviceLogBucket;
    }

    public void setDeviceLogBucket(String deviceLogBucket) {
        this.deviceLogBucket = deviceLogBucket;
    }
}
