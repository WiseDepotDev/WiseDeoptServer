package com.huicang.wise.domain.monitor;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * 视频截图记录实体 对应monitor_snapshot表，存储视频截图信息
 *
 * @author WiseDepot
 * @version 0.0.23
 * @since 2026-02-27
 */
public class MonitorSnapshot {

    /** 截图主键ID */
    private Long snapshotId;

    /** 设备ID */
    @NotNull(message = "设备ID不能为空")
    private Long deviceId;

    /** 截图文件ID（关联minio_file表） */
    @NotNull(message = "截图文件ID不能为空")
    private Long fileId;

    /** 截图时间 */
    @NotNull(message = "截图时间不能为空")
    private LocalDateTime captureTime;

    /** 创建时间 */
    private LocalDateTime createTime;

    /**
     * 获取截图主键ID
     *
     * @return 截图主键ID
     */
    public Long getSnapshotId() {
        return snapshotId;
    }

    /**
     * 设置截图主键ID
     *
     * @param snapshotId 截图主键ID
     */
    public void setSnapshotId(Long snapshotId) {
        this.snapshotId = snapshotId;
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
     * 获取截图文件ID
     *
     * @return 截图文件ID
     */
    public Long getFileId() {
        return fileId;
    }

    /**
     * 设置截图文件ID
     *
     * @param fileId 截图文件ID
     */
    public void setFileId(Long fileId) {
        this.fileId = fileId;
    }

    /**
     * 获取截图时间
     *
     * @return 截图时间
     */
    public LocalDateTime getCaptureTime() {
        return captureTime;
    }

    /**
     * 设置截图时间
     *
     * @param captureTime 截图时间
     */
    public void setCaptureTime(LocalDateTime captureTime) {
        this.captureTime = captureTime;
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
