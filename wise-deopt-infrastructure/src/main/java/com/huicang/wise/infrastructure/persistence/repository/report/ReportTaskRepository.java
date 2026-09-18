package com.huicang.wise.infrastructure.persistence.repository.report;

import com.huicang.wise.domain.report.ReportTask;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 报表任务仓储接口
 *
 * @author WiseDepot
 * @version 0.1.18
 * @since 2026-02-27
 */
@Repository
public interface ReportTaskRepository extends JpaRepository<ReportTask, Long> {

    /**
     * 根据任务ID查询报表任务
     *
     * @param taskId 任务ID
     * @return 报表任务
     */
    Optional<ReportTask> findByTaskId(Long taskId);

    /**
     * 根据任务名称查询报表任务
     *
     * @param reportName 任务名称
     * @return 报表任务列表
     */
    List<ReportTask> findByReportName(String reportName);

    /**
     * 根据任务状态查询报表任务列表
     *
     * @param status 任务状态
     * @return 报表任务列表
     */
    List<ReportTask> findByStatus(Short status);
}
