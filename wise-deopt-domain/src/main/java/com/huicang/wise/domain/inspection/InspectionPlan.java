package com.huicang.wise.domain.inspection;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * 巡检计划实体 对应inspection_plan表，存储巡检计划配置信息
 *
 * @author WiseDepot
 * @version 0.0.23
 * @since 2026-03-03
 */
public class InspectionPlan {

    /** 巡检计划主键ID */
    private Long planId;

    /** 计划名称 */
    @NotBlank(message = "计划名称不能为空")
    @Size(max = 100, message = "计划名称长度不能超过100个字符")
    private String planName;

    /** 执行巡检设备ID */
    @NotNull(message = "执行巡检设备ID不能为空")
    private Long deviceId;

    /** 定时表达式 */
    @NotBlank(message = "定时表达式不能为空")
    @Size(max = 100, message = "定时表达式长度不能超过100个字符")
    private String cronExpression;

    /** 状态：0：禁用 1：启用 */
    @NotNull(message = "状态不能为空")
    private Short status = 1;

    /** 最近执行时间 */
    private LocalDateTime lastExecuteTime;

    /** 下次执行时间 */
    private LocalDateTime nettExecuteTime;

    /** 创建者ID */
    @NotNull(message = "创建者ID不能为空")
    private Long createBy;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新者ID */
    @NotNull(message = "更新者ID不能为空")
    private Long updateBy;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /**
     * 获取巡检计划主键ID
     *
     * @return 巡检计划主键ID
     */
    public Long getPlanId() {
        return planId;
    }

    /**
     * 设置巡检计划主键ID
     *
     * @param planId 巡检计划主键ID
     */
    public void setPlanId(Long planId) {
        this.planId = planId;
    }

    /**
     * 获取计划名称
     *
     * @return 计划名称
     */
    public String getPlanName() {
        return planName;
    }

    /**
     * 设置计划名称
     *
     * @param planName 计划名称
     */
    public void setPlanName(String planName) {
        this.planName = planName;
    }

    /**
     * 获取执行巡检设备ID
     *
     * @return 执行巡检设备ID
     */
    public Long getDeviceId() {
        return deviceId;
    }

    /**
     * 设置执行巡检设备ID
     *
     * @param deviceId 执行巡检设备ID
     */
    public void setDeviceId(Long deviceId) {
        this.deviceId = deviceId;
    }

    /**
     * 获取定时表达式
     *
     * @return 定时表达式
     */
    public String getCronExpression() {
        return cronExpression;
    }

    /**
     * 设置定时表达式
     *
     * @param cronExpression 定时表达式
     */
    public void setCronExpression(String cronExpression) {
        this.cronExpression = cronExpression;
    }

    /**
     * 获取状态
     *
     * @return 状态
     */
    public Short getStatus() {
        return status;
    }

    /**
     * 设置状态
     *
     * @param status 状态
     */
    public void setStatus(Short status) {
        this.status = status;
    }

    /**
     * 获取最近执行时间
     *
     * @return 最近执行时间
     */
    public LocalDateTime getLastExecuteTime() {
        return lastExecuteTime;
    }

    /**
     * 设置最近执行时间
     *
     * @param lastExecuteTime 最近执行时间
     */
    public void setLastExecuteTime(LocalDateTime lastExecuteTime) {
        this.lastExecuteTime = lastExecuteTime;
    }

    /**
     * 获取下次执行时间
     *
     * @return 下次执行时间
     */
    public LocalDateTime getNettExecuteTime() {
        return nettExecuteTime;
    }

    /**
     * 设置下次执行时间
     *
     * @param nettExecuteTime 下次执行时间
     */
    public void setNettExecuteTime(LocalDateTime nettExecuteTime) {
        this.nettExecuteTime = nettExecuteTime;
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
