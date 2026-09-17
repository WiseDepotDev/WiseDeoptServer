package com.huicang.wise.domain.repository.report;

import com.huicang.wise.domain.report.ReportTask;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 报表仓储接口
 *
 * @author WiseDepot
 * @version 0.0.26
 * @since 2026-02-27
 */
@Repository
public interface ReportRepository extends JpaRepository<ReportTask, Long> {

    /**
     * 根据任务ID查询报表任务
     *
     * @param taskId 任务ID
     * @return 报表任务
     */
    Optional<ReportTask> findByTaskId(Long taskId);

    /**
     * 根据任务名称模糊查询报表任务
     *
     * @param reportName 任务名称（支持模糊匹配）
     * @param pageable 分页参数
     * @return 报表任务分页结果
     */
    @Query("SELECT t FROM ReportTask t WHERE t.reportName LIKE %:reportName%")
    Page<ReportTask> findByReportNameContaining(@Param("reportName") String reportName, Pageable pageable);

    /**
     * 根据任务状态查询报表任务
     *
     * @param status 任务状态
     * @return 报表任务列表
     */
    List<ReportTask> findByStatus(Short status);

    /**
     * 根据任务状态分页查询报表任务
     *
     * @param status 任务状态
     * @param pageable   分页参数
     * @return 报表任务分页结果
     */
    Page<ReportTask> findByStatus(Short status, Pageable pageable);

    /**
     * 分页查询报表任务列表
     *
     * @param pageable 分页参数
     * @return 报表任务分页结果
     */
    Page<ReportTask> findAll(Pageable pageable);

    /**
     * 根据时间范围查询报表任务
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 报表任务列表
     */
    List<ReportTask> findByCreateTimeBetween(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 根据时间范围分页查询报表任务
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @param pageable  分页参数
     * @return 报表任务分页结果
     */
    Page<ReportTask> findByCreateTimeBetween(LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);

    /**
     * 根据任务状态统计任务数量
     *
     * @param status 任务状态
     * @return 任务数量
     */
    long countByStatus(Short status);

    /**
     * 查询正在执行中的报表任务
     *
     * @param status 任务状态
     * @return 正在执行的任务列表
     */
    @Query("SELECT t FROM ReportTask t WHERE t.status = :status")
    List<ReportTask> findRunningTasks(@Param("status") Short status);

    /**
     * 查询执行失败的报表任务
     *
     * @param status 任务状态
     * @return 执行失败的任务列表
     */
    @Query("SELECT t FROM ReportTask t WHERE t.status = :status")
    List<ReportTask> findFailedTasks(@Param("status") Short status);
}
