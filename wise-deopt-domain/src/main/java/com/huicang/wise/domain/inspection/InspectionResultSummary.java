package com.huicang.wise.domain.inspection;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * 巡检结果汇总实体 对应inspection_result_summary表，存储巡检任务的汇总统计结果
 *
 * @author WiseDepot
 * @version 0.0.24
 * @since 2026-02-27
 */
public class InspectionResultSummary {

    /** 汇总结果主键ID */
    private Long resultId;

    /** 关联的巡检任务ID */
    @NotNull(message = "巡检任务ID不能为空")
    private Long taskId;

    /** 应盘数量（计划盘点项数） */
    @NotNull(message = "应盘数量不能为空")
    private Integer totalExpected;

    /** 实盘数量（实际盘点项数） */
    @NotNull(message = "实盘数量不能为空")
    private Integer totalScanned;

    /** 正常项数 */
    @NotNull(message = "正常项数不能为空")
    private Integer matchedCount = 0;

    /** 盘亏项数 */
    @NotNull(message = "盘亏项数不能为空")
    private Integer missingCount = 0;

    /** 盘盈项数 */
    @NotNull(message = "盘盈项数不能为空")
    private Integer extraCount = 0;

    /** 比对完成时间 */
    private LocalDateTime compareTime;

    /** 创建时间 */
    private LocalDateTime createTime;

    /**
     * 获取汇总结果主键ID
     *
     * @return 汇总结果主键ID
     */
    public Long getResultId() {
        return resultId;
    }

    /**
     * 设置汇总结果主键ID
     *
     * @param resultId 汇总结果主键ID
     */
    public void setResultId(Long resultId) {
        this.resultId = resultId;
    }

    /**
     * 获取关联的巡检任务ID
     *
     * @return 关联的巡检任务ID
     */
    public Long getTaskId() {
        return taskId;
    }

    /**
     * 设置关联的巡检任务ID
     *
     * @param taskId 关联的巡检任务ID
     */
    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    /**
     * 获取应盘数量
     *
     * @return 应盘数量
     */
    public Integer getTotalExpected() {
        return totalExpected;
    }

    /**
     * 设置应盘数量
     *
     * @param totalExpected 应盘数量
     */
    public void setTotalExpected(Integer totalExpected) {
        this.totalExpected = totalExpected;
    }

    /**
     * 获取实盘数量
     *
     * @return 实盘数量
     */
    public Integer getTotalScanned() {
        return totalScanned;
    }

    /**
     * 设置实盘数量
     *
     * @param totalScanned 实盘数量
     */
    public void setTotalScanned(Integer totalScanned) {
        this.totalScanned = totalScanned;
    }

    /**
     * 获取正常项数
     *
     * @return 正常项数
     */
    public Integer getMatchedCount() {
        return matchedCount;
    }

    /**
     * 设置正常项数
     *
     * @param matchedCount 正常项数
     */
    public void setMatchedCount(Integer matchedCount) {
        this.matchedCount = matchedCount;
    }

    /**
     * 获取盘亏项数
     *
     * @return 盘亏项数
     */
    public Integer getMissingCount() {
        return missingCount;
    }

    /**
     * 设置盘亏项数
     *
     * @param missingCount 盘亏项数
     */
    public void setMissingCount(Integer missingCount) {
        this.missingCount = missingCount;
    }

    /**
     * 获取盘盈项数
     *
     * @return 盘盈项数
     */
    public Integer getExtraCount() {
        return extraCount;
    }

    /**
     * 设置盘盈项数
     *
     * @param extraCount 盘盈项数
     */
    public void setExtraCount(Integer extraCount) {
        this.extraCount = extraCount;
    }

    /**
     * 获取比对完成时间
     *
     * @return 比对完成时间
     */
    public LocalDateTime getCompareTime() {
        return compareTime;
    }

    /**
     * 设置比對完成时间
     *
     * @param compareTime 比对完成时间
     */
    public void setCompareTime(LocalDateTime compareTime) {
        this.compareTime = compareTime;
    }

    /**
     * 获取创建时间
     *
     * @return 创建时间
     */
    public LocalDateTime getCreateTime() {
        return createTime;
    }

    /**
     * 设置创建时间
     *
     * @param createTime 创建时间
     */
    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
