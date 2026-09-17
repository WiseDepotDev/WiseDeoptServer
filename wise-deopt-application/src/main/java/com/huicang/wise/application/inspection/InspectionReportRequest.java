package com.huicang.wise.application.inspection;

import java.util.List;

import lombok.Data;

@Data
public class InspectionReportRequest {
    private String taskId;
    private Integer total_scanned;
    private Integer total_expected;
    private Integer match_count;
    private Integer surplus_count;
    private Integer loss_count;
    private Integer abnormal_count;
    private List<InspectionReportItem> details;
    private List<InspectionDifferenceVO> differences;
}
