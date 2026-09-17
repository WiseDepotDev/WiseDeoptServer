package com.huicang.wise.domain.inspection;

import java.io.Serializable;

/**
 * 巡检任务消息 (用于发送给设备)
 */
public class TaskMessage implements Serializable {
    private Long taskId;
    private Short taskType;
    private Float targetDistance;
    private Long planId;
    private Long warehouseId;

    public TaskMessage() {}

    public TaskMessage(Long taskId, Short taskType, Float targetDistance, Long planId, Long warehouseId) {
        this.taskId = taskId;
        this.taskType = taskType;
        this.targetDistance = targetDistance;
        this.planId = planId;
        this.warehouseId = warehouseId;
    }

    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }

    public Short getTaskType() { return taskType; }
    public void setTaskType(Short taskType) { this.taskType = taskType; }

    public Float getTargetDistance() { return targetDistance; }
    public void setTargetDistance(Float targetDistance) { this.targetDistance = targetDistance; }

    public Long getPlanId() { return planId; }
    public void setPlanId(Long planId) { this.planId = planId; }

    public Long getWarehouseId() { return warehouseId; }
    public void setWarehouseId(Long warehouseId) { this.warehouseId = warehouseId; }
}
