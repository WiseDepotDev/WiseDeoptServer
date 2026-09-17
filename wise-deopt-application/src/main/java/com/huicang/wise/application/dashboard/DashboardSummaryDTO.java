package com.huicang.wise.application.dashboard;

import com.huicang.wise.application.alert.AlertDTO;
import com.huicang.wise.application.inspection.InspectionTaskDTO;
import lombok.Data;

import java.util.List;

@Data
public class DashboardSummaryDTO {

    private Long inventoryTotal;

    private Long todayAlertCount;

    private Integer inspectionProgress;

    private Long deviceOnlineCount;

    private List<AlertDTO> unprocessedAlerts;

    private InspectionTaskDTO currentTask;
}
