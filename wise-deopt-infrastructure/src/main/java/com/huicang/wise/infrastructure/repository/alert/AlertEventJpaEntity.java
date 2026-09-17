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

    @Column(name = "source_module")
    private String sourceModule;

    @Column(name = "level", columnDefinition = "tinyint unsigned")
    private Short level;

    @Column(name = "title")
    private String title;

    @Column(name = "message")
    private String message;

    @Column(name = "status", columnDefinition = "tinyint unsigned")
    private Short status;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "create_time")
    private LocalDateTime createTime;

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

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }

    public String getSourceModule() {
        return sourceModule;
    }

    public void setSourceModule(String sourceModule) {
        this.sourceModule = sourceModule;
    }

    public Short getLevel() {
        return level;
    }

    public void setLevel(Short level) {
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

    public Short getStatus() {
        return status;
    }

    public void setStatus(Short status) {
        this.status = status;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
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

    public String getExtendedData() {
        return extendedData;
    }

    public void setExtendedData(String extendedData) {
        this.extendedData = extendedData;
    }
}
