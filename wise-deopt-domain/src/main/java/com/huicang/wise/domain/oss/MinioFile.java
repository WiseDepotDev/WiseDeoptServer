package com.huicang.wise.domain.oss;

import java.time.LocalDateTime;

/**
 * 类功能描述：MinIO文件元数据实体
 *
 * @author xingchentye
 * @date 2026-01-19
 * @modified xingchentye 2026-01-19 定义MinIO文件字段
 */
public class MinioFile {

    /**
     * 方法功能描述：文件主键ID
     */
    private Long fileId;

    /**
     * 方法功能描述：Bucket名称
     */
    private String bucket;

    /**
     * 方法功能描述：对象Key
     */
    private String objectKey;

    /**
     * 方法功能描述：文件大小
     */
    private Long size;

    /**
     * 方法功能描述：内容类型
     */
    private String contentType;

    /**
     * 方法功能描述：创建时间
     */
    private LocalDateTime createdAt;

    public Long getFileId() {
        return fileId;
    }

    public void setFileId(Long fileId) {
        this.fileId = fileId;
    }

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public String getObjectKey() {
        return objectKey;
    }

    public void setObjectKey(String objectKey) {
        this.objectKey = objectKey;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

