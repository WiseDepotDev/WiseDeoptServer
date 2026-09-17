package com.huicang.wise.application.inout;

import java.time.LocalDateTime;
import java.util.List;

public class StockOrderDTO {
    private Long orderId;
    private String orderNo;
    private Long warehouseId;
    private String warehouseName;
    private Short type;
    private String orderType; // Enum String
    private Short status;
    private String orderStatus; // Enum String
    private Integer totalItems;
    private String remark;
    private LocalDateTime createTime;
    private String createdAt; // String formatted
    private Long createBy;
    private LocalDateTime updateTime;
    private Long updateBy;
    private LocalDateTime submitTime;
    private Long submitBy;
    private String createdByName;
    private List<StockOrderItemDTO> items;

    public String getCreatedByName() {
        return createdByName;
    }

    public void setCreatedByName(String createdByName) {
        this.createdByName = createdByName;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public String getWarehouseName() {
        return warehouseName;
    }

    public void setWarehouseName(String warehouseName) {
        this.warehouseName = warehouseName;
    }

    public Short getOrderType() {
        return type;
    }

    public void setOrderType(Short type) {
        this.type = type;
    }

    public String getOrderTypeStr() {
        return orderType;
    }

    public void setOrderTypeStr(String orderType) {
        this.orderType = orderType;
    }

    public Short getOrderStatus() {
        return status;
    }

    public void setOrderStatus(Short status) {
        this.status = status;
    }

    public String getOrderStatusStr() {
        return orderStatus;
    }

    public void setOrderStatusStr(String orderStatus) {
        this.orderStatus = orderStatus;
    }

    public Integer getTotalItems() {
        return totalItems;
    }

    public void setTotalItems(Integer totalItems) {
        this.totalItems = totalItems;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public Long getCreateBy() {
        return createBy;
    }

    public void setCreateBy(Long createBy) {
        this.createBy = createBy;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public Long getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(Long updateBy) {
        this.updateBy = updateBy;
    }

    public LocalDateTime getSubmitTime() {
        return submitTime;
    }

    public void setSubmitTime(LocalDateTime submitTime) {
        this.submitTime = submitTime;
    }

    public Long getSubmitBy() {
        return submitBy;
    }

    public void setSubmitBy(Long submitBy) {
        this.submitBy = submitBy;
    }

    public List<StockOrderItemDTO> getItems() {
        return items;
    }

    public void setItems(List<StockOrderItemDTO> items) {
        this.items = items;
    }
}
