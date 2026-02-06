package com.huicang.wise.application.alert;

/**
 * 类功能描述：告警创建请求
 *
 * @author xingchentye
 * @date 2026-01-19
 * @modified xingchentye 2026-01-19 定义告警创建字段
 */
public class AlertCreateRequest {

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
     * 告警快照URL
     */
    private String snapshotUrl;

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

    public String getSnapshotUrl() {
        return snapshotUrl;
    }

    public void setSnapshotUrl(String snapshotUrl) {
        this.snapshotUrl = snapshotUrl;
    }
}

