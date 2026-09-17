package com.huicang.wise.application.inventory;

import java.util.List;

/**
 * 产品分页数据传输对象
 *
 * @author xingchentye
 * @date 2026-02-27
 */
public class ProductPageDTO {

    /**
     * 总记录数
     */
    private Long total;

    /**
     * 产品列表
     */
    private List<ProductDTO> rows;

    /**
     * 获取总记录数
     *
     * @return 总记录数
     */
    public Long getTotal() {
        return total;
    }

    /**
     * 设置总记录数
     *
     * @param total 总记录数
     */
    public void setTotal(Long total) {
        this.total = total;
    }

    /**
     * 获取产品列表
     *
     * @return 产品列表
     */
    public List<ProductDTO> getRows() {
        return rows;
    }

    /**
     * 设置产品列表
     *
     * @param rows 产品列表
     */
    public void setRows(List<ProductDTO> rows) {
        this.rows = rows;
    }
}