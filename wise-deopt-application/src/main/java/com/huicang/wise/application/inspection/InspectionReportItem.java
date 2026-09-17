package com.huicang.wise.application.inspection;

import lombok.Data;

@Data
public class InspectionReportItem {
    private String epc;
    private String tid;
    private String status; // normal, surplus, loss, abnormal
    private Long timestamp;
}
