package com.huicang.wise.application.device;

/**
 * 类功能描述：设备创建请求
 *
 * @author xingchentye
 * @date 2026-02-27
 */
public class DeviceCreateRequest {

    private String deviceCode;

    private String deviceName;

    private Short deviceType;

    private String ipAddress;

    private String remark;

    public String getDeviceCode() {
        return deviceCode;
    }

    public void setDeviceCode(String deviceCode) {
        this.deviceCode = deviceCode;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public Short getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(Short deviceType) {
        this.deviceType = deviceType;
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
}
