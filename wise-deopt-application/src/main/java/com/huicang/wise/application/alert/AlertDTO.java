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
     * 告警事件ID
     */
    private Long eventId;

    /**
     * 来源模块
     */
    private String sourceModule;

    /**
     * 告警等级
     */
    private Integer level;

    /**
     * 告警标题
     */
    private String title;

    /**
     * 告警内容
     */
    private String message;

    /**
     * 告警状态
     */
    private Integer status;

    /**
     * 是否仍处于活跃状态
     */
    private Boolean isActive;

    /**
     * 产生时间
     */
    private LocalDateTime createTime;

    /**
     * 解除时间
     */
    private LocalDateTime resolvedTime;

    /**
     * 解除者id
     */
    private Long resolvedBy;

    /**
     * 扩展信息
     */
    private String extendedData;

    /**
     * 告警快照URL
     */
    private String snapshotUrl;

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

    public String getSnapshotUrl() {
        return snapshotUrl;
    }

    public void setSnapshotUrl(String snapshotUrl) {
        this.snapshotUrl = snapshotUrl;
    }
}

