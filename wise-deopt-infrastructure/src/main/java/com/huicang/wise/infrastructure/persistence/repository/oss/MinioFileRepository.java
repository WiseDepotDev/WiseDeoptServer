package com.huicang.wise.infrastructure.persistence.repository.oss;

import com.huicang.wise.domain.oss.MinioFile;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * MinIO文件仓储接口
 *
 * @author WiseDepot
 * @version 0.1.19
 * @since 2026-02-27
 */
@Repository
public interface MinioFileRepository extends JpaRepository<MinioFile, Long> {

    /**
     * 根据上传人ID查询文件
     *
     * @param uploadBy 上传人ID
     * @return 文件列表
     */
    List<MinioFile> findByUploadBy(Long uploadBy);

    /**
     * 根据上传人ID分页查询文件
     *
     * @param uploadBy 上传人ID
     * @param pageable 分页参数
     * @return 文件分页结果
     */
    Page<MinioFile> findByUploadBy(Long uploadBy, Pageable pageable);

    /**
     * 分页查询文件列表
     *
     * @param pageable 分页参数
     * @return 文件分页结果
     */
    Page<MinioFile> findAll(Pageable pageable);

    /**
     * 根据时间范围查询文件
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 文件列表
     */
    List<MinioFile> findByUploadTimeBetween(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 根据时间范围分页查询文件
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param pageable 分页参数
     * @return 文件分页结果
     */
    Page<MinioFile> findByUploadTimeBetween(
            LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);

    /**
     * 检查文件ID是否存在
     *
     * @param fileId 文件ID
     * @return 是否存在
     */
    boolean existsByFileId(Long fileId);

    /**
     * 统计文件总大小
     *
     * @return 文件总大小（字节）
     */
    @Query(value = "SELECT SUM(file_size) FROM minio_file", nativeQuery = true)
    Long sumTotalFileSize();
}
