package com.huicang.wise.application.task;

import java.time.LocalDateTime;

/**
 * 类功能描述：任务更新请求
 *
 * @author xingchentye
 * @date 2026-01-22
 */
public class TaskUpdateRequest {

    private Long taskId;
    private String taskName;
    private Integer status;
    private LocalDateTime plannedStartTime;
    private String remark;

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
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

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
