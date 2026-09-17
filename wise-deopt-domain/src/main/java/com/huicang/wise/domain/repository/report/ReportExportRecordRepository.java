package com.huicang.wise.domain.repository.report;

import com.huicang.wise.domain.report.ReportExportRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 报表导出记录仓储接口
 *
 * @author WiseDepot
 * @version 0.0.22
 * @since 2026-02-27
 */
@Repository
public interface ReportExportRecordRepository extends JpaRepository<ReportExportRecord, Long> {

    /**
     * 根据任务ID查询导出记录列表
     *
     * @param taskId 任务ID
     * @return 导出记录列表
     */
    List<ReportExportRecord> findByTaskId(Long taskId);

    /**
     * 根据任务ID分页查询导出记录
     *
     * @param taskId  任务ID
     * @param pageable 分页参数
     * @return 导出记录分页结果
     */
    Page<ReportExportRecord> findByTaskId(Long taskId, Pageable pageable);

    /**
     * 根据导出时间范围查询导出记录
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 导出记录列表
     */
    List<ReportExportRecord> findByExportTimeBetween(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 根据导出时间范围分页查询导出记录
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @param pageable  分页参数
     * @return 导出记录分页结果
     */
    Page<ReportExportRecord> findByExportTimeBetween(LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);

    /**
     * 根据任务ID和导出时间范围查询导出记录
     *
     * @param taskId    任务ID
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 导出记录列表
     */
    @Query("SELECT r FROM ReportExportRecord r WHERE r.taskId = :taskId AND r.exportTime BETWEEN :startTime AND :endTime")
    List<ReportExportRecord> findByTaskIdAndExportTimeBetween(
            @Param("taskId") Long taskId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * 根据任务ID和导出时间范围分页查询导出记录
     *
     * @param taskId    任务ID
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @param pageable  分页参数
     * @return 导出记录分页结果
     */
    @Query("SELECT r FROM ReportExportRecord r WHERE r.taskId = :taskId AND r.exportTime BETWEEN :startTime AND :endTime")
    Page<ReportExportRecord> findByTaskIdAndExportTimeBetween(
            @Param("taskId") Long taskId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            Pageable pageable);

    /**
     * 根据任务ID删除导出记录
     *
     * @param taskId 任务ID
     */
    void deleteByTaskId(Long taskId);
}