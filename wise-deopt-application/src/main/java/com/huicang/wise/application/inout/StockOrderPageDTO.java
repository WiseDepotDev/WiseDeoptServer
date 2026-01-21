package com.huicang.wise.application.inout;

import java.util.List;

public class StockOrderPageDTO {

    private Long total;

    private List<StockOrderDTO> rows;

    public Long getTotal() {
        return total;
    }

    public void setTotal(Long total) {
        this.total = total;
    }

    public List<StockOrderDTO> getRows() {
        return rows;
    }

    public void setRows(List<StockOrderDTO> rows) {
        this.rows = rows;
    }
}
