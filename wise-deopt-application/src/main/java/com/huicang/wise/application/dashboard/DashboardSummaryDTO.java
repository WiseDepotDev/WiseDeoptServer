package com.huicang.wise.application.dashboard;

import com.huicang.wise.application.alert.AlertEventSummaryDTO;
import com.huicang.wise.application.task.TaskDTO;
import com.huicang.wise.application.inventory.InventoryDifferenceDTO;
import java.util.List;

public class DashboardSummaryDTO {

    private Long inventoryTotal;

    private Long todayAlertCount;

    private Integer inspectionProgress;

    private Integer deviceOnlineCount;

    private List<AlertEventSummaryDTO> unprocessedAlerts;
    
    private List<InventoryDifferenceDTO> recentDifferences;

    private TaskDTO currentTask;

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

    public List<AlertEventSummaryDTO> getUnprocessedAlerts() {
        return unprocessedAlerts;
    }

    public void setUnprocessedAlerts(List<AlertEventSummaryDTO> unprocessedAlerts) {
        this.unprocessedAlerts = unprocessedAlerts;
    }

    public List<InventoryDifferenceDTO> getRecentDifferences() {
        return recentDifferences;
    }

    public void setRecentDifferences(List<InventoryDifferenceDTO> recentDifferences) {
        this.recentDifferences = recentDifferences;
    }

    public TaskDTO getCurrentTask() {
        return currentTask;
    }

    public void setCurrentTask(TaskDTO currentTask) {
        this.currentTask = currentTask;
    }
}
