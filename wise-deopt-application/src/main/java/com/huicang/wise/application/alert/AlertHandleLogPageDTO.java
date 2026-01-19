package com.huicang.wise.application.alert;

import java.util.List;

public class AlertHandleLogPageDTO {

    private Long total;

    private List<AlertHandleLogDTO> rows;

    public Long getTotal() {
        return total;
    }

    public void setTotal(Long total) {
        this.total = total;
    }

    public List<AlertHandleLogDTO> getRows() {
        return rows;
    }

    public void setRows(List<AlertHandleLogDTO> rows) {
        this.rows = rows;
    }
}

