package com.huicang.wise.infrastructure.repository.inspection;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * 巡检结果汇总JPA实体
 * 对应inspection_result_summary表，存储巡检任务的汇总统计结果
 *
 * @author WiseDepot
 * @version 0.0.24
 * @since 2026-02-27
 */
@Entity
@Table(name = "inspection_result_summary", indexes = {
    @Index(name = "uk_task_id", columnList = "task_id", unique = true),
    @Index(name = "idx_compare_time", columnList = "compare_time"),
    @Index(name = "idx_create_time", columnList = "create_time")
})
public class InspectionResultSummaryJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "result_id")
    private Long resultId;

    @NotNull(message = "巡检任务ID不能为空")
    @Column(name = "task_id", nullable = false, unique = true)
    private Long taskId;

    @NotNull(message = "应盘数量不能为空")
    @Column(name = "total_expected", nullable = false)
    private Integer totalExpected;

    @NotNull(message = "实盘数量不能为空")
    @Column(name = "total_scanned", nullable = false)
    private Integer totalScanned;

    @NotNull(message = "正常项数不能为空")
    @Column(name = "matched_count", nullable = false)
    private Integer matchedCount = 0;

    @NotNull(message = "盘亏项数不能为空")
    @Column(name = "missing_count", nullable = false)
    private Integer missingCount = 0;

    @NotNull(message = "盘盈项数不能为空")
    @Column(name = "extra_count", nullable = false)
    private Integer extraCount = 0;

    @Column(name = "compare_time", nullable = false)
    private LocalDateTime compareTime;

    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    public Long getResultId() {
        return resultId;
    }

    public void setResultId(Long resultId) {
        this.resultId = resultId;
    }

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public Integer getTotalExpected() {
        return totalExpected;
    }

    public void setTotalExpected(Integer totalExpected) {
        this.totalExpected = totalExpected;
    }

    public Integer getTotalScanned() {
        return totalScanned;
    }

    public void setTotalScanned(Integer totalScanned) {
        this.totalScanned = totalScanned;
    }

    public Integer getMatchedCount() {
        return matchedCount;
    }

    public void setMatchedCount(Integer matchedCount) {
        this.matchedCount = matchedCount;
    }

    public Integer getMissingCount() {
        return missingCount;
    }

    public void setMissingCount(Integer missingCount) {
        this.missingCount = missingCount;
    }

    public Integer getExtraCount() {
        return extraCount;
    }

    public void setExtraCount(Integer extraCount) {
        this.extraCount = extraCount;
    }

    public LocalDateTime getCompareTime() {
        return compareTime;
    }

    public void setCompareTime(LocalDateTime compareTime) {
        this.compareTime = compareTime;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
