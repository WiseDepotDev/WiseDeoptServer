package com.huicang.wise.application.device;

public class DeviceCreateRequest {

    private String name;

    private String deviceCode;

    private Integer type;

    private String ipAddress;

    private String remark;

    private String rfidLocation;

    private Integer rfidReadRange;

    private String cameraStreamUrl;

    private String cameraLocation;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDeviceCode() {
        return deviceCode;
    }

    public void setDeviceCode(String deviceCode) {
        this.deviceCode = deviceCode;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
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

