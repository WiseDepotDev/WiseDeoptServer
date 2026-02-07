package com.huicang.wise.application.task;

import java.time.LocalDateTime;

/**
 * 类功能描述：任务创建请求
 *
 * @author xingchentye
 * @date 2026-01-22
 */
public class TaskCreateRequest {

    /**
     * 方法功能描述：任务名称
     */
    private String taskName;

    /**
     * 方法功能描述：任务类型
     */
    private Integer taskType;

    /**
     * 方法功能描述：计划开始时间
     */
    private LocalDateTime plannedStartTime;

    /**
     * 方法功能描述：备注
     */
    private String remark;

    /**
     * 方法功能描述：创建人
     */
    private String createdBy;

    /**
     * 方法功能描述：关联路线ID
     */
    private Long routeId;

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

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Long getRouteId() {
        return routeId;
    }

    public void setRouteId(Long routeId) {
        this.routeId = routeId;
    }
}
