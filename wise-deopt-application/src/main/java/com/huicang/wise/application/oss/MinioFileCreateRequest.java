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
     * 方法功能描述：存储桶名称
     */
    private String bucketName;

    /**
     * 方法功能描述：文件路径
     */
    private String filePath;

    /**
     * 方法功能描述：文件大小
     */
    private Long fileSize;

    /**
     * 方法功能描述：上传者ID
     */
    private Long uploadBy;

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public Long getUploadBy() {
        return uploadBy;
    }

    public void setUploadBy(Long uploadBy) {
        this.uploadBy = uploadBy;
    }
}
