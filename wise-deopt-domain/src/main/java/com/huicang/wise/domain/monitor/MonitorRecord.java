package com.huicang.wise.domain.monitor;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * 录像文件记录实体 对应monitor_record表，存储录像文件信息
 *
 * @author WiseDepot
 * @version 0.0.23
 * @since 2026-02-27
 */
public class MonitorRecord {

    /** 记录主键ID */
    private Long recordId;

    /** 设备ID */
    @NotNull(message = "设备ID不能为空")
    private Long deviceId;

    /** 录像文件ID（关联minio_file表） */
    @NotNull(message = "录像文件ID不能为空")
    private Long fileId;

    /** 录像开始时间 */
    @NotNull(message = "录像开始时间不能为空")
    private LocalDateTime startTime;

    /** 录像结束时间 */
    @NotNull(message = "录像结束时间不能为空")
    private LocalDateTime endTime;

    /** 创建时间 */
    private LocalDateTime createTime;

    /**
     * 获取记录主键ID
     *
     * @return 记录主键ID
     */
    public Long getRecordId() {
        return recordId;
    }

    /**
     * 设置记录主键ID
     *
     * @param recordId 记录主键ID
     */
    public void setRecordId(Long recordId) {
        this.recordId = recordId;
    }

    /**
     * 获取设备ID
     *
     * @return 设备ID
     */
    public Long getDeviceId() {
        return deviceId;
    }

    /**
     * 设置设备ID
     *
     * @param deviceId 设备ID
     */
    public void setDeviceId(Long deviceId) {
        this.deviceId = deviceId;
    }

    /**
     * 获取录像文件ID
     *
     * @return 录像文件ID
     */
    public Long getFileId() {
        return fileId;
    }

    /**
     * 设置录像文件ID
     *
     * @param fileId 录像文件ID
     */
    public void setFileId(Long fileId) {
        this.fileId = fileId;
    }

    /**
     * 获取录像开始时间
     *
     * @return 录像开始时间
     */
    public LocalDateTime getStartTime() {
        return startTime;
    }

    /**
     * 设置录像开始时间
     *
     * @param startTime 录像开始时间
     */
    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    /**
     * 获取录像结束时间
     *
     * @return 录像结束时间
     */
    public LocalDateTime getEndTime() {
        return endTime;
    }

    /**
     * 设置录像结束时间
     *
     * @param endTime 录像结束时间
     */
    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
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
