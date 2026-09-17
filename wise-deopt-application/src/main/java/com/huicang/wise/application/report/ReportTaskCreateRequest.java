package com.huicang.wise.application.report;

import java.time.LocalDateTime;

public class ReportTaskCreateRequest {

    private String reportName;
    private LocalDateTime timeRangeStart;
    private LocalDateTime timeRangeEnd;
    private String remark;

    public String getReportName() {
        return reportName;
    }

    public void setReportName(String reportName) {
        this.reportName = reportName;
    }

    public LocalDateTime getTimeRangeStart() {
        return timeRangeStart;
    }

    public void setTimeRangeStart(LocalDateTime timeRangeStart) {
        this.timeRangeStart = timeRangeStart;
    }

    public LocalDateTime getTimeRangeEnd() {
        return timeRangeEnd;
    }

    public void setTimeRangeEnd(LocalDateTime timeRangeEnd) {
        this.timeRangeEnd = timeRangeEnd;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
