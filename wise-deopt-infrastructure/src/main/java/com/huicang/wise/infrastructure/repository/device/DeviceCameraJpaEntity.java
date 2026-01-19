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

    @Column(name = "camera_stream_url")
    private String cameraStreamUrl;

    @Column(name = "camera_location")
    private String cameraLocation;

    public Long getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(Long deviceId) {
        this.deviceId = deviceId;
    }

    public String getCameraStreamUrl() {
        return cameraStreamUrl;
    }

    public void setCameraStreamUrl(String cameraStreamUrl) {
        this.cameraStreamUrl = cameraStreamUrl;
    }

    public String getCameraLocation() {
        return cameraLocation;
    }

    public void setCameraLocation(String cameraLocation) {
        this.cameraLocation = cameraLocation;
    }
}

