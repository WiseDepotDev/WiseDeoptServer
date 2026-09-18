package com.huicang.wise.application.device;

/**
 * 类功能描述：设备更新请求
 *
 * @author xingchentye
 * @date 2026-02-27
 */
public class DeviceUpdateRequest {

    private String deviceName;

    private String ipAddress;

    private Short deviceStatus;

    private String remark;

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public Short getDeviceStatus() {
        return deviceStatus;
    }

    public void setDeviceStatus(Short deviceStatus) {
        this.deviceStatus = deviceStatus;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
