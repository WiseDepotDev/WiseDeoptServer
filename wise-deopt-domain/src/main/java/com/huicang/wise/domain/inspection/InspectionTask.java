package com.huicang.wise.domain.inspection;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * 巡检任务实体 对应inspection_task表，存储巡检任务执行信息
 *
 * @author WiseDepot
 * @version 0.0.23
 * @since 2026-03-03
 */
public class InspectionTask {

    /** 巡检任务主键ID */
    private Long taskId;

    /** 关联的巡检计划ID */
    private Long planId;

    /** 仓库ID */
    private Long warehouseId;

    /** 巡检任务类型 0：定时任务 1：手动任务 */
    @NotNull(message = "任务类型不能为空")
    private Short taskType;

    /** 执行巡检设备ID */
    @NotNull(message = "执行巡检设备ID不能为空")
    private Long deviceId;

    /** 巡检目标距离（cm） */
    private Float targetDistance;

    /** 任务状态 0：待执行 1：执行中 2：已完成 3：异常终止 */
    @NotNull(message = "任务状态不能为空")
    private Short status = 0;

    /** 任务开始时间 */
    private LocalDateTime startTime;

    /** 任务结束时间 */
    private LocalDateTime endTime;

    /** 任务进度 (0-100) */
    private Integer progress = 0;

    /** 总项数 */
    private Integer totalItems = 0;

    /** 已盘项数 */
    private Integer inspectedItems = 0;

    /** 正常项数 */
    private Integer normalItems = 0;

    /** 异常项数 */
    private Integer abnormalItems = 0;

    /** 盘亏项数 */
    private Integer missingItems = 0;

    /** 盘盈项数 */
    private Integer extraItems = 0;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /**
     * 获取巡检任务主键ID
     *
     * @return 巡检任务主键ID
     */
    public Long getTaskId() {
        return taskId;
    }

    /**
     * 设置巡检任务主键ID
     *
     * @param taskId 巡检任务主键ID
     */
    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    /**
     * 获取关联的巡检计划ID
     *
     * @return 关联的巡检计划ID
     */
    public Long getPlanId() {
        return planId;
    }

    /**
     * 设置关联的巡检计划ID
     *
     * @param planId 关联的巡检计划ID
     */
    public void setPlanId(Long planId) {
        this.planId = planId;
    }

    /**
     * 获取仓库ID
     *
     * @return 仓库ID
     */
    public Long getWarehouseId() {
        return warehouseId;
    }

    /**
     * 设置仓库ID
     *
     * @param warehouseId 仓库ID
     */
    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    /**
     * 获取巡检任务类型
     *
     * @return 巡检任务类型
     */
    public Short getTaskType() {
        return taskType;
    }

    /**
     * 设置巡检任务类型
     *
     * @param taskType 巡检任务类型
     */
    public void setTaskType(Short taskType) {
        this.taskType = taskType;
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
     * 获取巡检目标距离
     *
     * @return 目标距离(cm)
     */
    public Float getTargetDistance() {
        return targetDistance;
    }

    /**
     * 设置巡检目标距离
     *
     * @param targetDistance 目标距离(cm)
     */
    public void setTargetDistance(Float targetDistance) {
        this.targetDistance = targetDistance;
    }

    /**
     * 获取任务状态
     *
     * @return 任务状态
     */
    public Short getStatus() {
        return status;
    }

    /**
     * 设置任务状态
     *
     * @param status 任务状态
     */
    public void setStatus(Short status) {
        this.status = status;
    }

    /**
     * 获取任务开始时间
     *
     * @return 任务开始时间
     */
    public LocalDateTime getStartTime() {
        return startTime;
    }

    /**
     * 设置任务开始时间
     *
     * @param startTime 任务开始时间
     */
    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    /**
     * 获取任务结束时间
     *
     * @return 任务结束时间
     */
    public LocalDateTime getEndTime() {
        return endTime;
    }

    /**
     * 设置任务结束时间
     *
     * @param endTime 任务结束时间
     */
    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    /**
     * 获取任务进度
     *
     * @return 进度 (0-100)
     */
    public Integer getProgress() {
        return progress;
    }

    /**
     * 设置任务进度
     *
     * @param progress 进度
     */
    public void setProgress(Integer progress) {
        this.progress = progress;
    }

    public Integer getTotalItems() {
        return totalItems;
    }

    public void setTotalItems(Integer totalItems) {
        this.totalItems = totalItems;
    }

    public Integer getInspectedItems() {
        return inspectedItems;
    }

    public void setInspectedItems(Integer inspectedItems) {
        this.inspectedItems = inspectedItems;
    }

    public Integer getNormalItems() {
        return normalItems;
    }

    public void setNormalItems(Integer normalItems) {
        this.normalItems = normalItems;
    }

    public Integer getAbnormalItems() {
        return abnormalItems;
    }

    public void setAbnormalItems(Integer abnormalItems) {
        this.abnormalItems = abnormalItems;
    }

    public Integer getMissingItems() {
        return missingItems;
    }

    public void setMissingItems(Integer missingItems) {
        this.missingItems = missingItems;
    }

    public Integer getExtraItems() {
        return extraItems;
    }

    public void setExtraItems(Integer extraItems) {
        this.extraItems = extraItems;
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
