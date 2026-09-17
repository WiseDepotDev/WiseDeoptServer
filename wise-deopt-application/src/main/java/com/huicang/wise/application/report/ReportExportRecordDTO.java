package com.huicang.wise.application.report;

import java.time.LocalDateTime;

public class ReportExportRecordDTO {

    private Long recordId;
    private Long taskId;
    private Long exportUserId;
    private String exportFormat;
    private LocalDateTime exportTime;
    private Long exportFileId;
    private String remark;

    public Long getRecordId() {
        return recordId;
    }

    public void setRecordId(Long recordId) {
        this.recordId = recordId;
    }

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public Long getExportUserId() {
        return exportUserId;
    }

    public void setExportUserId(Long exportUserId) {
        this.exportUserId = exportUserId;
    }

    public String getExportFormat() {
        return exportFormat;
    }

    public void setExportFormat(String exportFormat) {
        this.exportFormat = exportFormat;
    }

    public LocalDateTime getExportTime() {
        return exportTime;
    }

    public void setExportTime(LocalDateTime exportTime) {
        this.exportTime = exportTime;
    }

    public Long getExportFileId() {
        return exportFileId;
    }

    public void setExportFileId(Long exportFileId) {
        this.exportFileId = exportFileId;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
