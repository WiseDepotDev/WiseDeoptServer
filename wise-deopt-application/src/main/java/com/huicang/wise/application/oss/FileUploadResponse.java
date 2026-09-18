package com.huicang.wise.application.oss;

import java.time.LocalDateTime;

/**
 * 类功能描述：文件上传响应对象
 *
 * @author WiseDepot
 * @version 0.1.19
 * @since 2026-02-27
 */
public class FileUploadResponse {

    /** 文件ID */
    private Long fileId;

    /** 文件路径 */
    private String filePath;

    /** 文件大小（字节） */
    private Long fileSize;

    /** 访问URL */
    private String accessUrl;

    /** 上传时间 */
    private LocalDateTime uploadTime;

    /**
     * 获取文件ID
     *
     * @return 文件ID
     */
    public Long getFileId() {
        return fileId;
    }

    /**
     * 设置文件ID
     *
     * @param fileId 文件ID
     */
    public void setFileId(Long fileId) {
        this.fileId = fileId;
    }

    /**
     * 获取文件路径
     *
     * @return 文件路径
     */
    public String getFilePath() {
        return filePath;
    }

    /**
     * 设置文件路径
     *
     * @param filePath 文件路径
     */
    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    /**
     * 获取文件大小
     *
     * @return 文件大小
     */
    public Long getFileSize() {
        return fileSize;
    }

    /**
     * 设置文件大小
     *
     * @param fileSize 文件大小
     */
    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    /**
     * 获取访问URL
     *
     * @return 访问URL
     */
    public String getAccessUrl() {
        return accessUrl;
    }

    /**
     * 设置访问URL
     *
     * @param accessUrl 访问URL
     */
    public void setAccessUrl(String accessUrl) {
        this.accessUrl = accessUrl;
    }

    /**
     * 获取上传时间
     *
     * @return 上传时间
     */
    public LocalDateTime getUploadTime() {
        return uploadTime;
    }

    /**
     * 设置上传时间
     *
     * @param uploadTime 上传时间
     */
    public void setUploadTime(LocalDateTime uploadTime) {
        this.uploadTime = uploadTime;
    }
}
