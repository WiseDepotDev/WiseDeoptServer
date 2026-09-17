package com.huicang.wise.domain.oss;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * MinIO文件元数据实体
 * 对应minio_file表，存储文件元数据信息
 *
 * @author WiseDepot
 * @version 0.0.23
 * @since 2026-02-27
 */
@Entity
@Table(name = "minio_file", indexes = {
    @Index(name = "uk_bucket_path", columnList = "bucket_name,file_path", unique = true),
    @Index(name = "idx_upload_time", columnList = "upload_time"),
    @Index(name = "idx_upload_by", columnList = "upload_by")
})
public class MinioFile {

    /**
     * 文件主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "file_id")
    private Long fileId;

    /**
     * 存储桶名称
     */
    @NotBlank(message = "存储桶名称不能为空")
    @Size(max = 64, message = "存储桶名称长度不能超过64个字符")
    @Column(name = "bucket_name", nullable = false, length = 64)
    private String bucketName;

    /**
     * 文件路径
     */
    @NotBlank(message = "文件路径不能为空")
    @Size(max = 255, message = "文件路径长度不能超过255个字符")
    @Column(name = "file_path", nullable = false, length = 255)
    private String filePath;

    /**
     * 文件大小（字节）
     */
    @NotNull(message = "文件大小不能为空")
    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    /**
     * 上传时间
     */
    @Column(name = "upload_time", nullable = false, updatable = false)
    private LocalDateTime uploadTime;

    /**
     * 上传者ID
     */
    @NotNull(message = "上传者ID不能为空")
    @Column(name = "upload_by", nullable = false)
    private Long uploadBy;

    /**
     * 获取文件主键ID
     *
     * @return 文件主键ID
     */
    public Long getFileId() {
        return fileId;
    }

    /**
     * 设置文件主键ID
     *
     * @param fileId 文件主键ID
     */
    public void setFileId(Long fileId) {
        this.fileId = fileId;
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

    /**
     * 获取上传者ID
     *
     * @return 上传者ID
     */
    public Long getUploadBy() {
        return uploadBy;
    }

    /**
     * 设置上传者ID
     *
     * @param uploadBy 上传者ID
     */
    public void setUploadBy(Long uploadBy) {
        this.uploadBy = uploadBy;
    }
}
