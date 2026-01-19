package com.huicang.wise.infrastructure.repository.device;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "device_inspection_robot")
public class DeviceInspectionRobotJpaEntity {

    @Id
    @Column(name = "device_id")
    private Long deviceId;

    public Long getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(Long deviceId) {
        this.deviceId = deviceId;
    }
}

