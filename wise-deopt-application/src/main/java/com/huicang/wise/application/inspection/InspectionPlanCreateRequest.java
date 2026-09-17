package com.huicang.wise.application.inspection;

public class InspectionPlanCreateRequest {
    private String planName;
    private Long deviceId;
    private String cronExpression;

    public String getPlanName() { return planName; }
    public void setPlanName(String planName) { this.planName = planName; }
    public Long getDeviceId() { return deviceId; }
    public void setDeviceId(Long deviceId) { this.deviceId = deviceId; }
    public String getCronExpression() { return cronExpression; }
    public void setCronExpression(String cronExpression) { this.cronExpression = cronExpression; }
}