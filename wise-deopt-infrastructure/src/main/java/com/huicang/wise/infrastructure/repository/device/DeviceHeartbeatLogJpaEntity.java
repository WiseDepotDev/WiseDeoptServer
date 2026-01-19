package com.huicang.wise.infrastructure.repository.device;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "device_heartbeat_log")
public class DeviceHeartbeatLogJpaEntity {

    @Id
    @Column(name = "log_id")
    private Long logId;

    @Column(name = "device_id")
    private Long deviceId;

    @Column(name = "heartbeat_time")
    private LocalDateTime heartbeatTime;

    @Column(name = "remark")
    private String remark;

    public Long getLogId() {
        return logId;
    }

    public void setLogId(Long logId) {
        this.logId = logId;
    }

    public Long getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(Long deviceId) {
        this.deviceId = deviceId;
    }

    public LocalDateTime getHeartbeatTime() {
        return heartbeatTime;
    }

    public void setHeartbeatTime(LocalDateTime heartbeatTime) {
        this.heartbeatTime = heartbeatTime;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}

