package com.huicang.wise.application.report;

import java.time.LocalDateTime;

public class ReportTaskDTO {

    private Long taskId;
    private String reportName;
    private LocalDateTime timeRangeStart;
    private LocalDateTime timeRangeEnd;
    private Short status;
    private LocalDateTime generateTime;
    private Long generateFileId;
    private String remark;
    private LocalDateTime createTime;
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
