package com.huicang.wise.application.tag;

import java.util.List;

/**
 * 类功能描述：产品标签分页数据传输对象
 *
 * @author xingchentye
 * @date 2026-02-27
 */
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
