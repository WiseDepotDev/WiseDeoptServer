package com.huicang.wise.application.device;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

/**
 * 类功能描述：设备数据传输对象
 *
 * @author xingchentye
 * @date 2026-02-27
 */
public class DeviceDTO {

    private Long deviceId;

    private String deviceCode;

    private String deviceName;

    private Short deviceType;

    private String deviceTypeName;

    private String ipAddress;

    private Short deviceStatus;

    private String deviceStatusName;

    private LocalDateTime lastHeartbeat;

    private String remark;

    private String token;

    @JsonProperty("refreshToken")
    private String refreshToken;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Float moveSpeedCmS;

    private Float motorTrimA;

    private Float motorTrimB;

    private Float motorTrimC;

    private Float motorTrimD;

    public Long getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(Long deviceId) {
        this.deviceId = deviceId;
    }

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

    public String getDeviceTypeName() {
        return deviceTypeName;
    }

    public void setDeviceTypeName(String deviceTypeName) {
        this.deviceTypeName = deviceTypeName;
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

    public String getDeviceStatusName() {
        return deviceStatusName;
    }

    public void setDeviceStatusName(String deviceStatusName) {
        this.deviceStatusName = deviceStatusName;
    }

    public LocalDateTime getLastHeartbeat() {
        return lastHeartbeat;
    }

    public void setLastHeartbeat(LocalDateTime lastHeartbeat) {
        this.lastHeartbeat = lastHeartbeat;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public Float getMoveSpeedCmS() {
        return moveSpeedCmS;
    }

    public void setMoveSpeedCmS(Float moveSpeedCmS) {
        this.moveSpeedCmS = moveSpeedCmS;
    }

    public Float getMotorTrimA() {
        return motorTrimA;
    }

    public void setMotorTrimA(Float motorTrimA) {
        this.motorTrimA = motorTrimA;
    }

    public Float getMotorTrimB() {
        return motorTrimB;
    }

    public void setMotorTrimB(Float motorTrimB) {
        this.motorTrimB = motorTrimB;
    }

    public Float getMotorTrimC() {
        return motorTrimC;
    }

    public void setMotorTrimC(Float motorTrimC) {
        this.motorTrimC = motorTrimC;
    }

    public Float getMotorTrimD() {
        return motorTrimD;
    }

    public void setMotorTrimD(Float motorTrimD) {
        this.motorTrimD = motorTrimD;
    }
}
