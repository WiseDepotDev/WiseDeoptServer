package com.huicang.wise.infrastructure.repository.alert;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
@Entity
@Table(name = "alert_handle_log")
public class AlertHandleLogJpaEntity {

    @Id
    @Column(name = "log_id")
    private Long logId;

    @Column(name = "event_id")
    private Long eventId;

    @Column(name = "handler_id")
    private Long handlerId;

    @Column(name = "handler_name")
    private String handlerName;

    @Column(name = "goal_status")
    private Integer goalStatus;

    @Column(name = "goal_status_description")
    private String goalStatusDescription;

    @Column(name = "remark")
    private String remark;

    @Column(name = "handle_time")
    private LocalDateTime handleTime;

    public Long getLogId() {
        return logId;
    }

    public void setLogId(Long logId) {
        this.logId = logId;
    }

    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }

    public Long getHandlerId() {
        return handlerId;
    }

    public void setHandlerId(Long handlerId) {
        this.handlerId = handlerId;
    }

    public String getHandlerName() {
        return handlerName;
    }

    public void setHandlerName(String handlerName) {
        this.handlerName = handlerName;
    }

    public Integer getGoalStatus() {
        return goalStatus;
    }

    public void setGoalStatus(Integer goalStatus) {
        this.goalStatus = goalStatus;
    }

    public String getGoalStatusDescription() {
        return goalStatusDescription;
    }

    public void setGoalStatusDescription(String goalStatusDescription) {
        this.goalStatusDescription = goalStatusDescription;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public LocalDateTime getHandleTime() {
        return handleTime;
    }

    public void setHandleTime(LocalDateTime handleTime) {
        this.handleTime = handleTime;
    }
}
