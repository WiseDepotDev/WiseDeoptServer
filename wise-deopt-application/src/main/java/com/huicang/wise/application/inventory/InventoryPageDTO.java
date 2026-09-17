package com.huicang.wise.application.inventory;

import java.util.List;

/**
 * 类功能描述：库存分页响应对象
 *
 * @author xingchentye
 * @date 2026-02-26
 * @modified xingchentye 2026-02-26 创建库存分页响应对象
 */
public class InventoryPageDTO {

    /**
     * 字段功能描述：总记录数
     */
    private Long total;

    /**
     * 字段功能描述：库存明细列表
     */
    private List<InventoryDTO> rows;

    public Long getTotal() {
        return total;
    }

    public void setTotal(Long total) {
        this.total = total;
    }

    public List<InventoryDTO> getRows() {
        return rows;
    }

    public void setRows(List<InventoryDTO> rows) {
        this.rows = rows;
    }
}
