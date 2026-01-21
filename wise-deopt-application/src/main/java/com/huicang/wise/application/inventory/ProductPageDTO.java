package com.huicang.wise.application.inventory;

import java.util.List;

public class ProductPageDTO {

    private Long total;

    private List<ProductDTO> rows;

    public Long getTotal() {
        return total;
    }

    public void setTotal(Long total) {
        this.total = total;
    }

    public List<ProductDTO> getRows() {
        return rows;
    }

    public void setRows(List<ProductDTO> rows) {
        this.rows = rows;
    }
}
