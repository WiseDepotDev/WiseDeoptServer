package com.huicang.wise.infrastructure.repository.report;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

@Entity
@Table(name = "report_task", indexes = {
    @Index(name = "uk_report_name", columnList = "report_name", unique = true),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_create_time", columnList = "create_time"),
    @Index(name = "idx_time_range", columnList = "time_range_start,time_range_end"),
    @Index(name = "idx_generate_time", columnList = "generate_time"),
    @Index(name = "generate_file_id", columnList = "generate_file_id")
})
public class ReportTaskJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "task_id")
    private Long taskId;

    @NotBlank(message = "报表名称不能为空")
    @Size(max = 100, message = "报表名称长度不能超过100个字符")
    @Column(name = "report_name", nullable = false, length = 100)
    private String reportName;

    @NotNull(message = "数据起始时间不能为空")
    @Column(name = "time_range_start", nullable = false)
    private LocalDateTime timeRangeStart;

    @NotNull(message = "数据截止时间不能为空")
    @Column(name = "time_range_end", nullable = false)
    private LocalDateTime timeRangeEnd;

    @NotNull(message = "状态不能为空")
    @Column(name = "status", nullable = false, columnDefinition = "tinyint unsigned")
    private Short status = 0;

    @Column(name = "generate_time")
    private LocalDateTime generateTime;

    @Column(name = "generate_file_id")
    private Long generateFileId;

    @Size(max = 255, message = "备注长度不能超过255个字符")
    @Column(name = "remark", length = 255)
    private String remark;

    @NotNull(message = "创建时间不能为空")
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    @NotNull(message = "更新时间不能为空")
    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public String getReportName() {
        return reportName;
    }

    public void setReportName(String reportName) {
        this.reportName = reportName;
    }

    public LocalDateTime getTimeRangeStart() {
        return timeRangeStart;
    }

    public void setTimeRangeStart(LocalDateTime timeRangeStart) {
        this.timeRangeStart = timeRangeStart;
    }

    public LocalDateTime getTimeRangeEnd() {
        return timeRangeEnd;
    }

    public void setTimeRangeEnd(LocalDateTime timeRangeEnd) {
        this.timeRangeEnd = timeRangeEnd;
    }

    public Short getStatus() {
        return status;
    }

    public void setStatus(Short status) {
        this.status = status;
    }

    public LocalDateTime getGenerateTime() {
        return generateTime;
    }

    public void setGenerateTime(LocalDateTime generateTime) {
        this.generateTime = generateTime;
    }

    public Long getGenerateFileId() {
        return generateFileId;
    }

    public void setGenerateFileId(Long generateFileId) {
        this.generateFileId = generateFileId;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}
