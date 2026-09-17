package com.huicang.wise.domain.device;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * 监控摄像头设备实体
 * 对应device_camera表，存储摄像头设备信息
 *
 * @author WiseDepot
 * @version 0.0.23
 * @since 2026-02-27
 */
@Entity
@Table(name = "device_camera", indexes = {
    @Index(name = "uk_camera_stream_url", columnList = "stream_url", unique = true),
    @Index(name = "idx_camera_location", columnList = "location"),
    @Index(name = "idx_camera_create_time", columnList = "create_time")
})
public class DeviceCamera {

    /**
     * 设备ID（关联device_core表）
     */
    @Id
    @Column(name = "device_id")
    private Long deviceId;

    /**
     * 视频流URL
     */
    @NotBlank(message = "视频流URL不能为空")
    @Size(max = 255, message = "视频流URL长度不能超过255个字符")
    @Column(name = "stream_url", nullable = false, unique = true, length = 255)
    private String streamUrl;

    /**
     * 安装位置
     */
    @NotBlank(message = "安装位置不能为空")
    @Size(max = 100, message = "安装位置长度不能超过100个字符")
    @Column(name = "location", nullable = false, length = 100)
    private String location;

    /**
     * 创建者ID
     */
    @NotNull(message = "创建者ID不能为空")
    @Column(name = "create_by", nullable = false)
    private Long createBy;

    /**
     * 创建时间
     */
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    /**
     * 更新者ID
     */
    @NotNull(message = "更新者ID不能为空")
    @Column(name = "update_by", nullable = false)
    private Long updateBy;

    /**
     * 更新时间
     */
    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;

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
     * 获取视频流URL
     *
     * @return 视频流URL
     */
    public String getStreamUrl() {
        return streamUrl;
    }

    /**
     * 设置视频流URL
     *
     * @param streamUrl 视频流URL
     */
    public void setStreamUrl(String streamUrl) {
        this.streamUrl = streamUrl;
    }

    /**
     * 获取安装位置
     *
     * @return 安装位置
     */
    public String getLocation() {
        return location;
    }

    /**
     * 设置安装位置
     *
     * @param location 安装位置
     */
    public void setLocation(String location) {
        this.location = location;
    }

    /**
     * 获取创建者ID
     *
     * @return 创建者ID
     */
    public Long getCreateBy() {
        return createBy;
    }

    /**
     * 设置创建者ID
     *
     * @param createBy 创建者ID
     */
    public void setCreateBy(Long createBy) {
        this.createBy = createBy;
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

    /**
     * 获取更新者ID
     *
     * @return 更新者ID
     */
    public Long getUpdateBy() {
        return updateBy;
    }

    /**
     * 设置更新者ID
     *
     * @param updateBy 更新者ID
     */
    public void setUpdateBy(Long updateBy) {
        this.updateBy = updateBy;
    }

    /**
     * 获取更新时间
     *
     * @return 更新时间
     */
    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    /**
     * 设置更新时间
     *
     * @param updateTime 更新时间
     */
    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}
