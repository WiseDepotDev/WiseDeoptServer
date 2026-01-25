package com.huicang.wise.domain.task;

import java.time.LocalDateTime;

/**
 * 类功能描述：任务实体（盘点任务等）
 *
 * @author xingchentye
 * @date 2026-01-22
 */
public class Task {

    /**
     * 方法功能描述：任务ID
     */
    private Long taskId;

    /**
     * 方法功能描述：任务编码
     */
    private String taskCode;

    /**
     * 方法功能描述：任务名称
     */
    private String taskName;

    /**
     * 方法功能描述：任务类型（0:盘点任务, 1:巡检任务）
     */
    private Integer taskType;

    /**
     * 方法功能描述：任务状态（0:待开始, 1:进行中, 2:已完成, 3:已取消）
     */
    private Integer status;

    /**
     * 方法功能描述：计划开始时间
     */
    private LocalDateTime plannedStartTime;

    /**
     * 方法功能描述：实际开始时间
     */
    private LocalDateTime actualStartTime;

    /**
     * 方法功能描述：实际结束时间
     */
    private LocalDateTime actualEndTime;

    /**
     * 方法功能描述：创建人
     */
    private String createdBy;

    /**
     * 方法功能描述：创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 方法功能描述：备注
     */
    private String remark;

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public String getTaskCode() {
        return taskCode;
    }

    public void setTaskCode(String taskCode) {
        this.taskCode = taskCode;
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public Integer getTaskType() {
        return taskType;
    }

    public void setTaskType(Integer taskType) {
        this.taskType = taskType;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public LocalDateTime getPlannedStartTime() {
        return plannedStartTime;
    }

    public void setPlannedStartTime(LocalDateTime plannedStartTime) {
        this.plannedStartTime = plannedStartTime;
    }

    public LocalDateTime getActualStartTime() {
        return actualStartTime;
    }

    public void setActualStartTime(LocalDateTime actualStartTime) {
        this.actualStartTime = actualStartTime;
    }

    public LocalDateTime getActualEndTime() {
        return actualEndTime;
    }

    public void setActualEndTime(LocalDateTime actualEndTime) {
        this.actualEndTime = actualEndTime;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
