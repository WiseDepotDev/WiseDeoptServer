package com.huicang.wise.infrastructure.repository.device;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "device_camera")
public class DeviceCameraJpaEntity {

    @Id
    @Column(name = "device_id")
    private Long deviceId;

    @Column(name = "stream_url")
    private String streamUrl;

    @Column(name = "location")
    private String location;

    @Column(name = "create_time")
    private java.time.LocalDateTime createTime;

    @Column(name = "create_by")
    private Long createBy;

    @Column(name = "update_time")
    private java.time.LocalDateTime updateTime;

    @Column(name = "update_by")
    private Long updateBy;

    public Long getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(Long deviceId) {
        this.deviceId = deviceId;
    }

    public String getStreamUrl() {
        return streamUrl;
    }

    public void setStreamUrl(String streamUrl) {
        this.streamUrl = streamUrl;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public java.time.LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(java.time.LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public Long getCreateBy() {
        return createBy;
    }

    public void setCreateBy(Long createBy) {
        this.createBy = createBy;
    }

    public java.time.LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(java.time.LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public Long getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(Long updateBy) {
        this.updateBy = updateBy;
    }
}

