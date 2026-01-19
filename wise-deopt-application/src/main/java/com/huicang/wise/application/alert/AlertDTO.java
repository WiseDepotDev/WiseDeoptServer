package com.huicang.wise.application.alert;

import java.time.LocalDateTime;

/**
 * 类功能描述：告警响应对象
 *
 * @author xingchentye
 * @date 2026-01-19
 * @modified xingchentye 2026-01-19 定义告警响应字段
 */
public class AlertDTO {

    /**
     * 方法功能描述：告警事件ID
     */
    private Long eventId;

    /**
     * 方法功能描述：告警类型
     */
    private String alertType;

    /**
     * 方法功能描述：告警级别
     */
    private String alertLevel;

    /**
     * 方法功能描述：告警描述
     */
    private String description;

    /**
     * 方法功能描述：告警时间
     */
    private LocalDateTime alertTime;

    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }

    public String getAlertType() {
        return alertType;
    }

    public void setAlertType(String alertType) {
        this.alertType = alertType;
    }

    public String getAlertLevel() {
        return alertLevel;
    }

    public void setAlertLevel(String alertLevel) {
        this.alertLevel = alertLevel;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getAlertTime() {
        return alertTime;
    }

    public void setAlertTime(LocalDateTime alertTime) {
        this.alertTime = alertTime;
    }
}

