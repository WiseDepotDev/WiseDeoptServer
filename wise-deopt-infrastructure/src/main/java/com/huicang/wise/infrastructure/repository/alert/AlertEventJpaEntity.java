package com.huicang.wise.infrastructure.repository.alert;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * 类功能描述：告警事件JPA实体
 *
 * @author xingchentye
 * @date 2026-01-19
 * @modified xingchentye 2026-01-19 映射alert_event表
 */
@Entity
@Table(name = "alert_event")
public class AlertEventJpaEntity {

    @Id
    @Column(name = "event_id")
    private Long eventId;

    @Column(name = "alert_type")
    private String alertType;

    @Column(name = "alert_level")
    private String alertLevel;

    @Column(name = "description")
    private String description;

    @Column(name = "alert_time")
    private LocalDateTime alertTime;

    @Column(name = "source_module")
    private String sourceModule;

    @Column(name = "level")
    private Integer level;

    @Column(name = "title")
    private String title;

    @Column(name = "message")
    private String message;

    @Column(name = "status")
    private Integer status;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "resolved_time")
    private LocalDateTime resolvedTime;

    @Column(name = "resolved_by")
    private Long resolvedBy;

    @Column(name = "extended_data")
    private String extendedData;

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

    public String getSourceModule() {
        return sourceModule;
    }

    public void setSourceModule(String sourceModule) {
        this.sourceModule = sourceModule;
    }

    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public LocalDateTime getResolvedTime() {
        return resolvedTime;
    }

    public void setResolvedTime(LocalDateTime resolvedTime) {
        this.resolvedTime = resolvedTime;
    }

    public Long getResolvedBy() {
        return resolvedBy;
    }

    public void setResolvedBy(Long resolvedBy) {
        this.resolvedBy = resolvedBy;
    }

    @Column(name = "snapshot_url")
    private String snapshotUrl;

    public String getExtendedData() {
        return extendedData;
    }

    public void setExtendedData(String extendedData) {
        this.extendedData = extendedData;
    }

    public String getSnapshotUrl() {
        return snapshotUrl;
    }

    public void setSnapshotUrl(String snapshotUrl) {
        this.snapshotUrl = snapshotUrl;
    }
}
