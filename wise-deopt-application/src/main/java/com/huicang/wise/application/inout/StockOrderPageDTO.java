package com.huicang.wise.application.inout;

import java.util.List;

public class StockOrderPageDTO {

    private List<StockOrderDTO> content;
    private List<StockOrderDTO> rows; // 兼容旧版
    private Long totalElements;
    private Long total; // 兼容旧版
    private Integer number;
    private Integer size;
    private Integer totalPages;
    private Boolean hasNext;
    private Boolean hasPrevious;

    public List<StockOrderDTO> getContent() {
        return content;
    }

    public void setContent(List<StockOrderDTO> content) {
        this.content = content;
        this.rows = content; // 同步设置
    }

    public List<StockOrderDTO> getRows() {
        return rows;
    }

    public void setRows(List<StockOrderDTO> rows) {
        this.rows = rows;
        this.content = rows; // 同步设置
    }

    public Long getTotalElements() {
        return totalElements;
    }

    public void setTotalElements(Long totalElements) {
        this.totalElements = totalElements;
        this.total = totalElements; // 同步设置
    }

    public Long getTotal() {
        return total;
    }

    public void setTotal(Long total) {
        this.total = total;
        this.totalElements = total; // 同步设置
    }

    public Integer getNumber() {
        return number;
    }

    public void setNumber(Integer number) {
        this.number = number;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public Integer getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(Integer totalPages) {
        this.totalPages = totalPages;
    }

    public Boolean getHasNext() {
        return hasNext;
    }

    public void setHasNext(Boolean hasNext) {
        this.hasNext = hasNext;
    }

    public Boolean getHasPrevious() {
        return hasPrevious;
    }

    public void setHasPrevious(Boolean hasPrevious) {
        this.hasPrevious = hasPrevious;
    }
}
