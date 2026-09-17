package com.huicang.wise.domain.device;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.DynamicUpdate;
import java.time.LocalDateTime;

/**
 * 设备核心信息实体
 * 对应device_core表，存储设备基本信息
 *
 * @author WiseDepot
 * @version 0.0.23
 * @since 2026-02-27
 */
@Entity
@Table(name = "device_core", indexes = {
    @Index(name = "uk_device_code", columnList = "device_code", unique = true),
    @Index(name = "idx_device_type", columnList = "type"),
    @Index(name = "idx_device_status", columnList = "status"),
    @Index(name = "idx_device_create_time", columnList = "create_time"),
    @Index(name = "idx_device_last_heartbeat", columnList = "last_heartbeat")
})
@DynamicUpdate
public class DeviceCore {

    /**
     * 设备主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "device_id")
    private Long deviceId;

    /**
     * 设备名称
     */
    @NotBlank(message = "设备名称不能为空")
    @Size(max = 100, message = "设备名称长度不能超过100个字符")
    @Column(name = "name", nullable = false, length = 100)
    private String name = "未命名设备";

    /**
     * 设备唯一编号
     */
    @NotBlank(message = "设备编号不能为空")
    @Size(max = 64, message = "设备编号长度不能超过64个字符")
    @Column(name = "device_code", nullable = false, unique = true, length = 64)
    private String deviceCode;

    /**
     * 设备类型
     * 0：RFID读写器 1：摄像头 2：巡检小车
     */
    @NotNull(message = "设备类型不能为空")
    @Column(name = "type", nullable = false, columnDefinition = "tinyint unsigned")
    private Short type;

    /**
     * 设备IP地址
     */
    // @NotBlank(message = "设备IP地址不能为空")
    @Pattern(regexp = "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$", message = "IP地址格式不正确")
    @Column(name = "ip_address", nullable = true, length = 39)
    private String ipAddress;

    /**
     * 设备状态
     * 0：离线 1：在线 2：故障
     */
    @NotNull(message = "设备状态不能为空")
    @Column(name = "status", nullable = false, columnDefinition = "tinyint unsigned")
    private Short status = 0;

    /**
     * 上次心跳时间
     */
    @Column(name = "last_heartbeat")
    private LocalDateTime lastHeartbeat;

    /**
     * 备注信息
     */
    @Size(max = 255, message = "备注长度不能超过255个字符")
    @Column(name = "remark", length = 255)
    private String remark;

    /**
     * 创建者ID
     */
    @Column(name = "create_by", nullable = false)
    private Long createBy;

    /**
     * 创建时间
     */
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    /**
     * 更新者ID
     */
    @Column(name = "update_by", nullable = false)
    private Long updateBy;

    /**
     * 更新时间
     */
    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;

    /**
     * 获取设备主键ID
     *
     * @return 设备主键ID
     */
    public Long getDeviceId() {
        return deviceId;
    }

    /**
     * 设置设备主键ID
     *
     * @param deviceId 设备主键ID
     */
    public void setDeviceId(Long deviceId) {
        this.deviceId = deviceId;
    }

    /**
     * 获取设备名称
     *
     * @return 设备名称
     */
    public String getName() {
        return name;
    }

    /**
     * 设置设备名称
     *
     * @param name 设备名称
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * 获取设备唯一编号
     *
     * @return 设备唯一编号
     */
    public String getDeviceCode() {
        return deviceCode;
    }

    /**
     * 设置设备唯一编号
     *
     * @param deviceCode 设备唯一编号
     */
    public void setDeviceCode(String deviceCode) {
        this.deviceCode = deviceCode;
    }

    /**
     * 获取设备类型
     *
     * @return 设备类型
     */
    public Short getType() {
        return type;
    }

    /**
     * 设置设备类型
     *
     * @param type 设备类型
     */
    public void setType(Short type) {
        this.type = type;
    }

    /**
     * 获取设备IP地址
     *
     * @return 设备IP地址
     */
    public String getIpAddress() {
        return ipAddress;
    }

    /**
     * 设置设备IP地址
     *
     * @param ipAddress 设备IP地址
     */
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    /**
     * 获取设备状态
     *
     * @return 设备状态
     */
    public Short getStatus() {
        return status;
    }

    /**
     * 设置设备状态
     *
     * @param status 设备状态
     */
    public void setStatus(Short status) {
        this.status = status;
    }

    /**
     * 获取上次心跳时间
     *
     * @return 上次心跳时间
     */
    public LocalDateTime getLastHeartbeat() {
        return lastHeartbeat;
    }

    /**
     * 设置上次心跳时间
     *
     * @param lastHeartbeat 上次心跳时间
     */
    public void setLastHeartbeat(LocalDateTime lastHeartbeat) {
        this.lastHeartbeat = lastHeartbeat;
    }

    /**
     * 获取备注信息
     *
     * @return 备注信息
     */
    public String getRemark() {
        return remark;
    }

    /**
     * 设置备注信息
     *
     * @param remark 备注信息
     */
    public void setRemark(String remark) {
        this.remark = remark;
    }

    /**
     * 获取创建者ID
     *
     * @return 创建者ID
     */
    public Long getCreateBy() {
        return createBy;
    }

    /**
     * 设置创建者ID
     *
     * @param createBy 创建者ID
     */
    public void setCreateBy(Long createBy) {
        this.createBy = createBy;
    }

    /**
     * 获取创建时间
     *
     * @return 创建时间
     */
    public LocalDateTime getCreateTime() {
        return createTime;
    }

    /**
     * 设置创建时间
     *
     * @param createTime 创建时间
     */
    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    /**
     * 获取更新者ID
     *
     * @return 更新者ID
     */
    public Long getUpdateBy() {
        return updateBy;
    }

    /**
     * 设置更新者ID
     *
     * @param updateBy 更新者ID
     */
    public void setUpdateBy(Long updateBy) {
        this.updateBy = updateBy;
    }

    /**
     * 获取更新时间
     *
     * @return 更新时间
     */
    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    /**
     * 设置更新时间
     *
     * @param updateTime 更新时间
     */
    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}
