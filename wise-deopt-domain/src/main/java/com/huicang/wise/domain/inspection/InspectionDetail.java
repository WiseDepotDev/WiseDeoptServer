package com.huicang.wise.domain.inspection;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * 巡检明细实体
 * 对应inspection_detail表，存储巡检过程中的详细记录
 *
 * @author WiseDepot
 * @version 0.0.23
 * @since 2026-03-03
 */
@Entity
@Table(name = "inspection_detail", indexes = {
    @Index(name = "idx_task_id", columnList = "task_id"),
    @Index(name = "idx_tag_id", columnList = "tag_id"),
    @Index(name = "idx_scan_time", columnList = "scan_time"),
    @Index(name = "idx_matched", columnList = "matched"),
    @Index(name = "idx_rfid", columnList = "rfid")
})
public class InspectionDetail {

    /**
     * 巡检明细主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "detail_id")
    private Long detailId;

    /**
     * 对应巡检任务ID
     */
    @NotNull(message = "巡检任务ID不能为空")
    @Column(name = "task_id", nullable = false)
    private Long taskId;

    /**
     * 扫描到的RFID标签ID
     */
    @Column(name = "tag_id")
    private Long tagId;

    /**
     * 扫描到的原始RFID
     */
    @NotBlank(message = "RFID不能为空")
    @Size(max = 128, message = "RFID长度不能超过128个字符")
    @Column(name = "rfid", nullable = false, length = 128)
    private String rfid;

    /**
     * 扫描时间
     */
    @NotNull(message = "扫描时间不能为空")
    @Column(name = "scan_time", nullable = false)
    private LocalDateTime scanTime;

    /**
     * 是否匹配系统库存：0：不匹配 1：匹配 (Deprecated, use status instead)
     */
    @NotNull(message = "匹配状态不能为空")
    @Column(name = "matched", nullable = false, columnDefinition = "tinyint unsigned")
    private Short matched = 0;

    /**
     * 状态: normal, surplus, loss, abnormal
     */
    @Column(name = "status", length = 32)
    private String status;

    /**
     * TID
     */
    @Column(name = "tid", length = 128)
    private String tid;

    /**
     * 备注
     */
    @Size(max = 255, message = "备注长度不能超过255个字符")
    @Column(name = "remark", length = 255)
    private String remark;

    /**
     * 获取巡检明细主键ID
     *
     * @return 巡检明细主键ID
     */
    public Long getDetailId() {
        return detailId;
    }

    /**
     * 设置巡检明细主键ID
     *
     * @param detailId 巡检明细主键ID
     */
    public void setDetailId(Long detailId) {
        this.detailId = detailId;
    }

    /**
     * 获取对应巡检任务ID
     *
     * @return 对应巡检任务ID
     */
    public Long getTaskId() {
        return taskId;
    }

    /**
     * 设置对应巡检任务ID
     *
     * @param taskId 对应巡检任务ID
     */
    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    /**
     * 获取扫描到的RFID标签ID
     *
     * @return 扫描到的RFID标签ID
     */
    public Long getTagId() {
        return tagId;
    }

    /**
     * 设置扫描到的RFID标签ID
     *
     * @param tagId 扫描到的RFID标签ID
     */
    public void setTagId(Long tagId) {
        this.tagId = tagId;
    }

    /**
     * 获取扫描到的原始RFID
     *
     * @return 扫描到的原始RFID
     */
    public String getRfid() {
        return rfid;
    }

    /**
     * 设置扫描到的原始RFID
     *
     * @param rfid 扫描到的原始RFID
     */
    public void setRfid(String rfid) {
        this.rfid = rfid;
    }

    /**
     * 获取扫描时间
     *
     * @return 扫描时间
     */
    public LocalDateTime getScanTime() {
        return scanTime;
    }

    /**
     * 设置扫描时间
     *
     * @param scanTime 扫描时间
     */
    public void setScanTime(LocalDateTime scanTime) {
        this.scanTime = scanTime;
    }

    /**
     * 获取是否匹配系统库存
     *
     * @return 是否匹配系统库存
     */
    public Short getMatched() {
        return matched;
    }

    /**
     * 设置是否匹配系统库存
     *
     * @param matched 是否匹配系统库存
     */
    public void setMatched(Short matched) {
        this.matched = matched;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTid() {
        return tid;
    }

    public void setTid(String tid) {
        this.tid = tid;
    }

    /**
     * 获取备注
     *
     * @return 备注
     */
    public String getRemark() {
        return remark;
    }

    /**
     * 设置备注
     *
     * @param remark 备注
     */
    public void setRemark(String remark) {
        this.remark = remark;
    }
}
