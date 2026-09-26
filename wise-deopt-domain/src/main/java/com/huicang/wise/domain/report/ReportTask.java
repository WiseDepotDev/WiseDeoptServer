package com.huicang.wise.domain.report;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public class ReportTask {

    private Long taskId;

    @NotBlank(message = "报表名称不能为空")
    @Size(max = 100, message = "报表名称长度不能超过100个字符")
    private String reportName;

    @NotNull(message = "数据起始时间不能为空")
    private LocalDateTime timeRangeStart;

    @NotNull(message = "数据截止时间不能为空")
    private LocalDateTime timeRangeEnd;

    @NotNull(message = "状态不能为空")
    private Short status = 0;

    private LocalDateTime generateTime;

    private Long generateFileId;

    @Size(max = 255, message = "备注长度不能超过255个字符")
    private String remark;

    @NotNull(message = "创建时间不能为空")
    private LocalDateTime createTime;

    @NotNull(message = "更新时间不能为空")
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
