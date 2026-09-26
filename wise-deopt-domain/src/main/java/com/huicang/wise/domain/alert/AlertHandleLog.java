package com.huicang.wise.domain.alert;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * 告警处理日志实体 对应alert_handle_log表，存储告警处理过程记录
 *
 * @author WiseDepot
 * @version 0.0.24
 * @since 2026-02-27
 */
public class AlertHandleLog {

    /** 处理日志主键ID */
    private Long logId;

    /** 关联告警id */
    @NotNull(message = "关联告警id不能为空")
    private Long eventId;

    /** 操作人id */
    @NotNull(message = "操作人id不能为空")
    private Long handlerId;

    /** 目标状态 0：未处理 1：处理中 2：已处理 3：已忽略 */
    @NotNull(message = "目标状态不能为空")
    private Short goalStatus;

    /** 操作备注 */
    @Size(max = 255, message = "操作备注长度不能超过255个字符")
    private String remark;

    /** 操作时间 */
    @NotNull(message = "操作时间不能为空")
    private LocalDateTime handleTime;

    /**
     * 获取处理日志主键ID
     *
     * @return 处理日志主键ID
     */
    public Long getLogId() {
        return logId;
    }

    /**
     * 设置处理日志主键ID
     *
     * @param logId 处理日志主键ID
     */
    public void setLogId(Long logId) {
        this.logId = logId;
    }

    /**
     * 获取关联告警id
     *
     * @return 关联告警id
     */
    public Long getEventId() {
        return eventId;
    }

    /**
     * 设置关联告警id
     *
     * @param eventId 关联告警id
     */
    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }

    /**
     * 获取操作人id
     *
     * @return 操作人id
     */
    public Long getHandlerId() {
        return handlerId;
    }

    /**
     * 设置操作人id
     *
     * @param handlerId 操作人id
     */
    public void setHandlerId(Long handlerId) {
        this.handlerId = handlerId;
    }

    /**
     * 获取目标状态
     *
     * @return 目标状态
     */
    public Short getGoalStatus() {
        return goalStatus;
    }

    /**
     * 设置目标状态
     *
     * @param goalStatus 目标状态
     */
    public void setGoalStatus(Short goalStatus) {
        this.goalStatus = goalStatus;
    }

    /**
     * 获取操作备注
     *
     * @return 操作备注
     */
    public String getRemark() {
        return remark;
    }

    /**
     * 设置操作备注
     *
     * @param remark 操作备注
     */
    public void setRemark(String remark) {
        this.remark = remark;
    }

    /**
     * 获取操作时间
     *
     * @return 操作时间
     */
    public LocalDateTime getHandleTime() {
        return handleTime;
    }

    /**
     * 设置操作时间
     *
     * @param handleTime 操作时间
     */
    public void setHandleTime(LocalDateTime handleTime) {
        this.handleTime = handleTime;
    }
}
