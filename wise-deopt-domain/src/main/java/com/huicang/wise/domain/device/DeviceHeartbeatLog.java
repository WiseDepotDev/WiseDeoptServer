package com.huicang.wise.domain.device;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * 设备心跳日志实体
 * 对应device_heartbeat_log表，记录设备心跳信息
 *
 * @author WiseDepot
 * @version 0.0.23
 * @since 2026-02-27
 */
@Entity
@Table(name = "device_heartbeat_log", indexes = {
    @Index(name = "idx_heartbeat_device_id", columnList = "device_id"),
    @Index(name = "idx_heartbeat_time", columnList = "heartbeat_time")
})
public class DeviceHeartbeatLog {

    /**
     * 日志主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long logId;

    /**
     * 设备ID
     */
    @NotNull(message = "设备ID不能为空")
    @Column(name = "device_id", nullable = false)
    private Long deviceId;

    /**
     * 心跳时间
     */
    @NotNull(message = "心跳时间不能为空")
    @Column(name = "heartbeat_time", nullable = false)
    private LocalDateTime heartbeatTime;

    /**
     * 附加信息
     */
    @Size(max = 255, message = "附加信息长度不能超过255个字符")
    @Column(name = "remark", length = 255)
    private String remark;

    /**
     * 获取日志主键ID
     *
     * @return 日志主键ID
     */
    public Long getLogId() {
        return logId;
    }

    /**
     * 设置日志主键ID
     *
     * @param logId 日志主键ID
     */
    public void setLogId(Long logId) {
        this.logId = logId;
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
     * 获取心跳时间
     *
     * @return 心跳时间
     */
    public LocalDateTime getHeartbeatTime() {
        return heartbeatTime;
    }

    /**
     * 设置心跳时间
     *
     * @param heartbeatTime 心跳时间
     */
    public void setHeartbeatTime(LocalDateTime heartbeatTime) {
        this.heartbeatTime = heartbeatTime;
    }

    /**
     * 获取附加信息
     *
     * @return 附加信息
     */
    public String getRemark() {
        return remark;
    }

    /**
     * 设置附加信息
     *
     * @param remark 附加信息
     */
    public void setRemark(String remark) {
        this.remark = remark;
    }
}
