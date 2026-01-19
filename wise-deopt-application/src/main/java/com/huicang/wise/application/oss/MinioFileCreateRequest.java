package com.huicang.wise.application.oss;

/**
 * 类功能描述：MinIO文件记录创建请求
 *
 * @author xingchentye
 * @date 2026-01-19
 * @modified xingchentye 2026-01-19 定义文件记录创建字段
 */
public class MinioFileCreateRequest {

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
}

