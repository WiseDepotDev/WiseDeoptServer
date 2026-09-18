package com.huicang.wise.application.inspection;

import java.time.LocalDateTime;

public class InspectionTaskDTO {
    private Long taskId;
    private Long planId;
    private Short taskType;
    private Long deviceId;
    private Float targetDistance;
    private Short status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    private Integer progress;
    private Integer totalItems;
    private Integer inspectedItems;
    private Integer normalItems;
    private Integer abnormalItems;
    private Integer missingItems;
    private Integer extraItems;

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public Long getPlanId() {
        return planId;
    }

    public void setPlanId(Long planId) {
        this.planId = planId;
    }

    public Short getTaskType() {
        return taskType;
    }

    public void setTaskType(Short taskType) {
        this.taskType = taskType;
    }

    public Long getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(Long deviceId) {
        this.deviceId = deviceId;
    }

    public Float getTargetDistance() {
        return targetDistance;
    }

    public void setTargetDistance(Float targetDistance) {
        this.targetDistance = targetDistance;
    }

    public Short getStatus() {
        return status;
    }

    public void setStatus(Short status) {
        this.status = status;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
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

    public Integer getProgress() {
        return progress;
    }

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

    // Additional fields for frontend display
    private String taskCode;
    private String planName;
    private String taskTypeDesc;
    private Long warehouseId;
    private String warehouseName;
    private String deviceName; // robotName
    private String statusDesc;

    public String getTaskCode() {
        return taskCode;
    }

    public void setTaskCode(String taskCode) {
        this.taskCode = taskCode;
    }

    public String getPlanName() {
        return planName;
    }

    public void setPlanName(String planName) {
        this.planName = planName;
    }

    public String getTaskTypeDesc() {
        return taskTypeDesc;
    }

    public void setTaskTypeDesc(String taskTypeDesc) {
        this.taskTypeDesc = taskTypeDesc;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public String getWarehouseName() {
        return warehouseName;
    }

    public void setWarehouseName(String warehouseName) {
        this.warehouseName = warehouseName;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getStatusDesc() {
        return statusDesc;
    }

    public void setStatusDesc(String statusDesc) {
        this.statusDesc = statusDesc;
    }
}
