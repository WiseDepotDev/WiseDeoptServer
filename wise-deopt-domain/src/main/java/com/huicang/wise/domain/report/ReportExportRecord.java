package com.huicang.wise.domain.report;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

@Entity
@Table(name = "report_export_record", indexes = {
    @Index(name = "idx_task_id", columnList = "task_id"),
    @Index(name = "idx_export_user_id", columnList = "export_user_id"),
    @Index(name = "idx_export_time", columnList = "export_time"),
    @Index(name = "idx_export_format", columnList = "export_format"),
    @Index(name = "uk_export_file_id", columnList = "export_file_id", unique = true)
})
public class ReportExportRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "record_id")
    private Long recordId;

    @NotNull(message = "报表任务ID不能为空")
    @Column(name = "task_id", nullable = false)
    private Long taskId;

    @NotNull(message = "导出人ID不能为空")
    @Column(name = "export_user_id", nullable = false)
    private Long exportUserId;

    @NotBlank(message = "导出格式不能为空")
    @Size(max = 10, message = "导出格式长度不能超过10个字符")
    @Column(name = "export_format", nullable = false, length = 10)
    private String exportFormat;

    @NotNull(message = "导出时间不能为空")
    @Column(name = "export_time", nullable = false)
    private LocalDateTime exportTime;

    @NotNull(message = "导出文件ID不能为空")
    @Column(name = "export_file_id", nullable = false)
    private Long exportFileId;

    @Size(max = 255, message = "备注长度不能超过255个字符")
    @Column(name = "remark", length = 255)
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
