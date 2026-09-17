package com.huicang.wise.application.oss;

import org.springframework.web.multipart.MultipartFile;

/**
 * 类功能描述：文件上传请求对象
 *
 * @author WiseDepot
 * @version 0.1.19
 * @since 2026-02-27
 */
public class FileUploadRequest {

    /**
     * 上传的文件
     */
    private MultipartFile file;

    /**
     * 存储桶名称
     */
    private String bucketName;

    /**
     * 获取上传的文件
     *
     * @return 上传的文件
     */
    public MultipartFile getFile() {
        return file;
    }

    /**
     * 设置上传的文件
     *
     * @param file 上传的文件
     */
    public void setFile(MultipartFile file) {
        this.file = file;
    }

    /**
     * 获取存储桶名称
     *
     * @return 存储桶名称
     */
    public String getBucketName() {
        return bucketName;
    }

    /**
     * 设置存储桶名称
     *
     * @param bucketName 存储桶名称
     */
    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }
}
