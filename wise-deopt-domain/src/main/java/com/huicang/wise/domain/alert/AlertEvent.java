package com.huicang.wise.domain.alert;

import java.time.LocalDateTime;

/**
 * 类功能描述：告警事件实体
 *
 * @author xingchentye
 * @date 2026-01-19
 * @modified xingchentye 2026-01-19 定义告警事件字段
 */
public class AlertEvent {

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

    /**
     * 方法功能描述：获取告警事件ID
     *
     * @return 告警事件ID
     */
    public Long getEventId() {
        return eventId;
    }

    /**
     * 方法功能描述：设置告警事件ID
     *
     * @param eventId 告警事件ID
     * @return 无
     */
    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }

    /**
     * 方法功能描述：获取告警类型
     *
     * @return 告警类型
     */
    public String getAlertType() {
        return alertType;
    }

    /**
     * 方法功能描述：设置告警类型
     *
     * @param alertType 告警类型
     * @return 无
     */
    public void setAlertType(String alertType) {
        this.alertType = alertType;
    }

    /**
     * 方法功能描述：获取告警级别
     *
     * @return 告警级别
     */
    public String getAlertLevel() {
        return alertLevel;
    }

    /**
     * 方法功能描述：设置告警级别
     *
     * @param alertLevel 告警级别
     * @return 无
     */
    public void setAlertLevel(String alertLevel) {
        this.alertLevel = alertLevel;
    }

    /**
     * 方法功能描述：获取告警描述
     *
     * @return 告警描述
     */
    public String getDescription() {
        return description;
    }

    /**
     * 方法功能描述：设置告警描述
     *
     * @param description 告警描述
     * @return 无
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * 方法功能描述：获取告警时间
     *
     * @return 告警时间
     */
    public LocalDateTime getAlertTime() {
        return alertTime;
    }

    /**
     * 方法功能描述：设置告警时间
     *
     * @param alertTime 告警时间
     * @return 无
     */
    public void setAlertTime(LocalDateTime alertTime) {
        this.alertTime = alertTime;
    }
}
