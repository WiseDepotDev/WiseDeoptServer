package com.huicang.wise.application.tag;

import java.util.List;

public class ProductTagPageDTO {

    private Long total;

    private List<ProductTagDTO> rows;

    public Long getTotal() {
        return total;
    }

    public void setTotal(Long total) {
        this.total = total;
    }

    public List<ProductTagDTO> getRows() {
        return rows;
    }

    public void setRows(List<ProductTagDTO> rows) {
        this.rows = rows;
    }
}

