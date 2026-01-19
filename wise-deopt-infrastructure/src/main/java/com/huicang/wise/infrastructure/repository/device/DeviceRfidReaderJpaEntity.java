package com.huicang.wise.infrastructure.repository.device;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "device_rfid_reader")
public class DeviceRfidReaderJpaEntity {

    @Id
    @Column(name = "device_id")
    private Long deviceId;

    @Column(name = "rfid_location")
    private String rfidLocation;

    @Column(name = "rfid_read_range")
    private Integer rfidReadRange;

    @Column(name = "camera_location")
    private String cameraLocation;

    public Long getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(Long deviceId) {
        this.deviceId = deviceId;
    }

    public String getRfidLocation() {
        return rfidLocation;
    }

    public void setRfidLocation(String rfidLocation) {
        this.rfidLocation = rfidLocation;
    }

    public Integer getRfidReadRange() {
        return rfidReadRange;
    }

    public void setRfidReadRange(Integer rfidReadRange) {
        this.rfidReadRange = rfidReadRange;
    }

    public String getCameraLocation() {
        return cameraLocation;
    }

    public void setCameraLocation(String cameraLocation) {
        this.cameraLocation = cameraLocation;
    }
}

