package com.huicang.wise.domain.repository.auth;

import com.huicang.wise.domain.auth.KeyAccessAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 访问密钥审计日志仓储接口
 *
 * @author WiseDepot
 * @version 0.0.22
 * @since 2026-02-27
 */
@Repository
public interface KeyAccessAuditLogRepository extends JpaRepository<KeyAccessAuditLog, Long> {

    /**
     * 根据访问密钥查询审计日志
     *
     * @param accessKey 访问密钥
     * @return 审计日志列表
     */
    List<KeyAccessAuditLog> findByAccessKey(String accessKey);

    /**
     * 根据访问密钥分页查询审计日志
     *
     * @param accessKey 访问密钥
     * @param pageable  分页参数
     * @return 审计日志分页结果
     */
    Page<KeyAccessAuditLog> findByAccessKey(String accessKey, Pageable pageable);

    /**
     * 根据用户ID查询审计日志
     *
     * @param userId 用户ID
     * @return 审计日志列表
     */
    List<KeyAccessAuditLog> findByUserId(Long userId);

    /**
     * 根据用户ID分页查询审计日志
     *
     * @param userId   用户ID
     * @param pageable 分页参数
     * @return 审计日志分页结果
     */
    Page<KeyAccessAuditLog> findByUserId(Long userId, Pageable pageable);

    /**
     * 根据用户ID删除审计日志
     *
     * @param userId 用户ID
     */
    void deleteByUserId(Long userId);

    /**
     * 根据请求时间范围查询审计日志
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 审计日志列表
     */
    List<KeyAccessAuditLog> findByRequestTimeBetween(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 根据请求时间范围分页查询审计日志
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @param pageable  分页参数
     * @return 审计日志分页结果
     */
    Page<KeyAccessAuditLog> findByRequestTimeBetween(LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);

    /**
     * 根据访问密钥和请求时间范围查询审计日志
     *
     * @param accessKey 访问密钥
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 审计日志列表
     */
    @Query("SELECT a FROM KeyAccessAuditLog a WHERE a.accessKey = :accessKey AND a.requestTime BETWEEN :startTime AND :endTime")
    List<KeyAccessAuditLog> findByAccessKeyAndRequestTimeBetween(
            @Param("accessKey") String accessKey,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * 根据访问密钥和请求时间范围分页查询审计日志
     *
     * @param accessKey 访问密钥
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @param pageable  分页参数
     * @return 审计日志分页结果
     */
    @Query("SELECT a FROM KeyAccessAuditLog a WHERE a.accessKey = :accessKey AND a.requestTime BETWEEN :startTime AND :endTime")
    Page<KeyAccessAuditLog> findByAccessKeyAndRequestTimeBetween(
            @Param("accessKey") String accessKey,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            Pageable pageable);
}