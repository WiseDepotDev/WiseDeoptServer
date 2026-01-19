package com.huicang.wise.infrastructure.repository.oss;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * 类功能描述：MinIO文件JPA实体
 *
 * @author xingchentye
 * @date 2026-01-19
 * @modified xingchentye 2026-01-19 映射minio_file表
 */
@Entity
@Table(name = "minio_file")
public class MinioFileJpaEntity {

    @Id
    @Column(name = "file_id")
    private Long fileId;

    @Column(name = "bucket")
    private String bucket;

    @Column(name = "object_key")
    private String objectKey;

    @Column(name = "size")
    private Long size;

    @Column(name = "content_type")
    private String contentType;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /**
     * 方法功能描述：获取文件主键ID
     *
     * @return 文件主键ID
     */
    public Long getFileId() {
        return fileId;
    }

    /**
     * 方法功能描述：设置文件主键ID
     *
     * @param fileId 文件主键ID
     * @return 无
     */
    public void setFileId(Long fileId) {
        this.fileId = fileId;
    }

    /**
     * 方法功能描述：获取Bucket名称
     *
     * @return Bucket名称
     */
    public String getBucket() {
        return bucket;
    }

    /**
     * 方法功能描述：设置Bucket名称
     *
     * @param bucket Bucket名称
     * @return 无
     */
    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    /**
     * 方法功能描述：获取对象Key
     *
     * @return 对象Key
     */
    public String getObjectKey() {
        return objectKey;
    }

    /**
     * 方法功能描述：设置对象Key
     *
     * @param objectKey 对象Key
     * @return 无
     */
    public void setObjectKey(String objectKey) {
        this.objectKey = objectKey;
    }

    /**
     * 方法功能描述：获取文件大小
     *
     * @return 文件大小
     */
    public Long getSize() {
        return size;
    }

    /**
     * 方法功能描述：设置文件大小
     *
     * @param size 文件大小
     * @return 无
     */
    public void setSize(Long size) {
        this.size = size;
    }

    /**
     * 方法功能描述：获取内容类型
     *
     * @return 内容类型
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * 方法功能描述：设置内容类型
     *
     * @param contentType 内容类型
     * @return 无
     */
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    /**
     * 方法功能描述：获取创建时间
     *
     * @return 创建时间
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * 方法功能描述：设置创建时间
     *
     * @param createdAt 创建时间
     * @return 无
     */
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

