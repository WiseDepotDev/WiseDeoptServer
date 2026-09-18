package com.huicang.wise.application.alert;

import java.time.LocalDateTime;

/**
 * 类功能描述：设备离线告警请求
 *
 * @author WiseDepot
 * @version 0.1.17
 * @since 2026-02-27
 */
public class DeviceOfflineRequest {

    private Long deviceId;
    private String deviceName;
    private String deviceType;
    private LocalDateTime lastHeartbeatTime;

    public Long getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(Long deviceId) {
        this.deviceId = deviceId;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    public LocalDateTime getLastHeartbeatTime() {
        return lastHeartbeatTime;
    }

    public void setLastHeartbeatTime(LocalDateTime lastHeartbeatTime) {
        this.lastHeartbeatTime = lastHeartbeatTime;
    }
}
