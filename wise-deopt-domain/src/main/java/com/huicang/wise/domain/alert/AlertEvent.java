package com.huicang.wise.domain.alert;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * 告警事件实体 对应alert_event表，存储系统告警事件信息
 *
 * @author WiseDepot
 * @version 0.0.24
 * @since 2026-02-27
 */
public class AlertEvent {

    /** 告警事件主键ID */
    private Long eventId;

    /** 来源模块 */
    @NotBlank(message = "来源模块不能为空")
    @Size(max = 50, message = "来源模块长度不能超过50个字符")
    private String sourceModule;

    /** 告警等级 1：提示 2：一般 3：严重 4：紧急 */
    @NotNull(message = "告警等级不能为空")
    private Short level;

    /** 告警标题 */
    @NotBlank(message = "告警标题不能为空")
    @Size(max = 100, message = "告警标题长度不能超过100个字符")
    private String title;

    /** 告警内容 */
    @NotBlank(message = "告警内容不能为空")
    @Size(max = 255, message = "告警内容长度不能超过255个字符")
    private String message;

    /** 告警状态 0：未处理 1：处理中 2：已处理 3：已忽略 */
    @NotNull(message = "告警状态不能为空")
    private Short status = 0;

    /** 是否仍处于活跃状态 */
    @NotNull(message = "活跃状态不能为空")
    private Boolean isActive = true;

    /** 产生时间 */
    @NotNull(message = "产生时间不能为空")
    private LocalDateTime createTime;

    /** 解除时间 */
    private LocalDateTime resolvedTime;

    /** 解除者id */
    private Long resolvedBy;

    /** 扩展信息 */
    private String extendedData;

    /**
     * 获取告警事件主键ID
     *
     * @return 告警事件主键ID
     */
    public Long getEventId() {
        return eventId;
    }

    /**
     * 设置告警事件主键ID
     *
     * @param eventId 告警事件主键ID
     */
    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }

    /**
     * 获取来源模块
     *
     * @return 来源模块
     */
    public String getSourceModule() {
        return sourceModule;
    }

    /**
     * 设置来源模块
     *
     * @param sourceModule 来源模块
     */
    public void setSourceModule(String sourceModule) {
        this.sourceModule = sourceModule;
    }

    /**
     * 获取告警等级
     *
     * @return 告警等级
     */
    public Short getLevel() {
        return level;
    }

    /**
     * 设置告警等级
     *
     * @param level 告警等级
     */
    public void setLevel(Short level) {
        this.level = level;
    }

    /**
     * 获取告警标题
     *
     * @return 告警标题
     */
    public String getTitle() {
        return title;
    }

    /**
     * 设置告警标题
     *
     * @param title 告警标题
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * 获取告警内容
     *
     * @return 告警内容
     */
    public String getMessage() {
        return message;
    }

    /**
     * 设置告警内容
     *
     * @param message 告警内容
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * 获取告警状态
     *
     * @return 告警状态
     */
    public Short getStatus() {
        return status;
    }

    /**
     * 设置告警状态
     *
     * @param status 告警状态
     */
    public void setStatus(Short status) {
        this.status = status;
    }

    /**
     * 获取是否仍处于活跃状态
     *
     * @return 是否仍处于活跃状态
     */
    public Boolean getIsActive() {
        return isActive;
    }

    /**
     * 设置是否仍处于活跃状态
     *
     * @param isActive 是否仍处于活跃状态
     */
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    /**
     * 获取产生时间
     *
     * @return 产生时间
     */
    public LocalDateTime getCreateTime() {
        return createTime;
    }

    /**
     * 设置产生时间
     *
     * @param createTime 产生时间
     */
    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    /**
     * 获取解除时间
     *
     * @return 解除时间
     */
    public LocalDateTime getResolvedTime() {
        return resolvedTime;
    }

    /**
     * 设置解除时间
     *
     * @param resolvedTime 解除时间
     */
    public void setResolvedTime(LocalDateTime resolvedTime) {
        this.resolvedTime = resolvedTime;
    }

    /**
     * 获取解除者id
     *
     * @return 解除者id
     */
    public Long getResolvedBy() {
        return resolvedBy;
    }

    /**
     * 设置解除者id
     *
     * @param resolvedBy 解除者id
     */
    public void setResolvedBy(Long resolvedBy) {
        this.resolvedBy = resolvedBy;
    }

    /**
     * 获取扩展信息
     *
     * @return 扩展信息
     */
    public String getExtendedData() {
        return extendedData;
    }

    /**
     * 设置扩展信息
     *
     * @param extendedData 扩展信息
     */
    public void setExtendedData(String extendedData) {
        this.extendedData = extendedData;
    }
}
