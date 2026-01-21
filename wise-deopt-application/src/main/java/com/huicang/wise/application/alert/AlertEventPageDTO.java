package com.huicang.wise.application.alert;

import java.util.List;

public class AlertEventPageDTO {

    private Long total;

    private List<AlertEventSummaryDTO> rows;

    public Long getTotal() {
        return total;
    }

    public void setTotal(Long total) {
        this.total = total;
    }

    public List<AlertEventSummaryDTO> getRows() {
        return rows;
    }

    public void setRows(List<AlertEventSummaryDTO> rows) {
        this.rows = rows;
    }
}
