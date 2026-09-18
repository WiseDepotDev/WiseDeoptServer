package com.huicang.wise.application.inspection;

public class InspectionTaskCreateRequest {
    private Long planId;
    // private String taskType; // Deprecated/Defaulted
    private Long warehouseId;
    private Long deviceId;
    private Float targetDistance;

    public Long getPlanId() {
        return planId;
    }

    public void setPlanId(Long planId) {
        this.planId = planId;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    // public String getTaskType() { return taskType; }
    // public void setTaskType(String taskType) { this.taskType = taskType; }

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
}
