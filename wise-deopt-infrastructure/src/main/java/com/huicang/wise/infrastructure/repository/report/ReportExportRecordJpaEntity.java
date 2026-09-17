package com.huicang.wise.infrastructure.repository.report;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "report_export_record", indexes = {
    @Index(name = "idx_task_id", columnList = "task_id"),
    @Index(name = "idx_export_user_id", columnList = "export_user_id"),
    @Index(name = "idx_export_time", columnList = "export_time"),
    @Index(name = "idx_export_format", columnList = "export_format"),
    @Index(name = "uk_export_file_id", columnList = "export_file_id", unique = true)
})
public class ReportExportRecordJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "record_id")
    private Long recordId;

    @Column(name = "task_id", nullable = false)
    private Long taskId;

    @Column(name = "export_user_id", nullable = false)
    private Long exportUserId;

    @Column(name = "export_format", nullable = false, length = 10)
    private String exportFormat;

    @Column(name = "export_time", nullable = false)
    private LocalDateTime exportTime;

    @Column(name = "export_file_id", nullable = false)
    private Long exportFileId;

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
