package com.huicang.wise.domain.inout;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

@Entity
@Table(name = "stock_order", indexes = {
    @Index(name = "uk_order_no", columnList = "order_no", unique = true),
    @Index(name = "idx_warehouse_id", columnList = "warehouse_id"),
    @Index(name = "idx_type", columnList = "type"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_create_time", columnList = "create_time"),
    @Index(name = "idx_submit_time", columnList = "submit_time"),
    @Index(name = "create_by", columnList = "create_by"),
    @Index(name = "update_by", columnList = "update_by"),
    @Index(name = "submit_by", columnList = "submit_by")
})
public class StockOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Long orderId;

    @NotBlank(message = "单号不能为空")
    @Size(max = 64, message = "单号长度不能超过64个字符")
    @Column(name = "order_no", nullable = false, length = 64)
    private String orderNo;

    @NotNull(message = "仓库ID不能为空")
    @Column(name = "warehouse_id", nullable = false)
    private Long warehouseId;

    @NotNull(message = "单据类型不能为空")
    @Column(name = "type", nullable = false, columnDefinition = "tinyint unsigned")
    private Short type;

    @NotNull(message = "明细数不能为空")
    @Column(name = "total_items", nullable = false)
    private Integer totalItems;

    @NotNull(message = "单据状态不能为空")
    @Column(name = "status", nullable = false, columnDefinition = "tinyint unsigned")
    private Short status = 0;

    @Size(max = 255, message = "备注长度不能超过255个字符")
    @Column(name = "remark", length = 255)
    private String remark;

    @NotNull(message = "创建者ID不能为空")
    @Column(name = "create_by", nullable = false)
    private Long createBy;

    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    @NotNull(message = "更新者ID不能为空")
    @Column(name = "update_by", nullable = false)
    private Long updateBy;

    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;

    @Column(name = "submit_time")
    private LocalDateTime submitTime;

    @Column(name = "submit_by")
    private Long submitBy;

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

    public Short getType() {
        return type;
    }

    public void setType(Short type) {
        this.type = type;
    }

    public Integer getTotalItems() {
        return totalItems;
    }

    public void setTotalItems(Integer totalItems) {
        this.totalItems = totalItems;
    }

    public Short getStatus() {
        return status;
    }

    public void setStatus(Short status) {
        this.status = status;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public Long getCreateBy() {
        return createBy;
    }

    public void setCreateBy(Long createBy) {
        this.createBy = createBy;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public Long getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(Long updateBy) {
        this.updateBy = updateBy;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
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
}
