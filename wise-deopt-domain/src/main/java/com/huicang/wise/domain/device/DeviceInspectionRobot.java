package com.huicang.wise.domain.device;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * 智能巡检小车设备实体
 * 对应device_inspection_robot表，存储巡检小车设备信息
 *
 * @author WiseDepot
 * @version 0.0.23
 * @since 2026-02-27
 */
@Entity
@Table(name = "device_inspection_robot", indexes = {
    @Index(name = "idx_robot_last_task_time", columnList = "last_task_time"),
    @Index(name = "idx_robot_create_time", columnList = "create_time")
})
public class DeviceInspectionRobot {

    /**
     * 设备ID（关联device_core表）
     */
    @Id
    @Column(name = "device_id")
    private Long deviceId;

    /**
     * 最近执行任务时间
     */
    @Column(name = "last_task_time")
    private LocalDateTime lastTaskTime;

    /**
     * 创建者ID
     */
    @NotNull(message = "创建者ID不能为空")
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
    @NotNull(message = "更新者ID不能为空")
    @Column(name = "update_by", nullable = false)
    private Long updateBy;

    /**
     * 更新时间
     */
    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;

    /**
     * 移动速度 (cm/s)
     */
    @Column(name = "move_speed_cm_s")
    private Float moveSpeedCmS;

    /**
     * 左前电机微调
     */
    @Column(name = "motor_trim_a")
    private Float motorTrimA;

    /**
     * 右前电机微调
     */
    @Column(name = "motor_trim_b")
    private Float motorTrimB;

    /**
     * 左后电机微调
     */
    @Column(name = "motor_trim_c")
    private Float motorTrimC;

    /**
     * 右后电机微调
     */
    @Column(name = "motor_trim_d")
    private Float motorTrimD;

    /**
     * 获取设备ID
     *
     * @return 设备ID
     */
    public Long getDeviceId() {
        return deviceId;
    }

    /**
     * 获取移动速度
     * @return 移动速度
     */
    public Float getMoveSpeedCmS() {
        return moveSpeedCmS;
    }

    /**
     * 设置移动速度
     * @param moveSpeedCmS 移动速度
     */
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

    /**
     * 设置设备ID
     *
     * @param deviceId 设备ID
     */
    public void setDeviceId(Long deviceId) {
        this.deviceId = deviceId;
    }

    /**
     * 获取最近执行任务时间
     *
     * @return 最近执行任务时间
     */
    public LocalDateTime getLastTaskTime() {
        return lastTaskTime;
    }

    /**
     * 设置最近执行任务时间
     *
     * @param lastTaskTime 最近执行任务时间
     */
    public void setLastTaskTime(LocalDateTime lastTaskTime) {
        this.lastTaskTime = lastTaskTime;
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
