package com.huicang.wise.domain.report;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public class ReportExportRecord {

    private Long recordId;

    @NotNull(message = "报表任务ID不能为空")
    private Long taskId;

    @NotNull(message = "导出人ID不能为空")
    private Long exportUserId;

    @NotBlank(message = "导出格式不能为空")
    @Size(max = 10, message = "导出格式长度不能超过10个字符")
    private String exportFormat;

    @NotNull(message = "导出时间不能为空")
    private LocalDateTime exportTime;

    @NotNull(message = "导出文件ID不能为空")
    private Long exportFileId;

    @Size(max = 255, message = "备注长度不能超过255个字符")
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
