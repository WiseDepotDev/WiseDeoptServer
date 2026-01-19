package com.huicang.wise.application.dashboard;

public class DashboardSummaryDTO {

    private Long inventoryTotal;

    private Long todayAlertCount;

    private Integer inspectionProgress;

    private Integer deviceOnlineCount;

    public Long getInventoryTotal() {
        return inventoryTotal;
    }

    public void setInventoryTotal(Long inventoryTotal) {
        this.inventoryTotal = inventoryTotal;
    }

    public Long getTodayAlertCount() {
        return todayAlertCount;
    }

    public void setTodayAlertCount(Long todayAlertCount) {
        this.todayAlertCount = todayAlertCount;
    }

    public Integer getInspectionProgress() {
        return inspectionProgress;
    }

    public void setInspectionProgress(Integer inspectionProgress) {
        this.inspectionProgress = inspectionProgress;
    }

    public Integer getDeviceOnlineCount() {
        return deviceOnlineCount;
    }

    public void setDeviceOnlineCount(Integer deviceOnlineCount) {
        this.deviceOnlineCount = deviceOnlineCount;
    }
}
