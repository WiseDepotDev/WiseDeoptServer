package com.huicang.wise.infrastructure.repository.inout;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "stock_order", indexes = {
    @Index(name = "uk_order_no", columnList = "order_no", unique = true),
    @Index(name = "idx_type", columnList = "type"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_create_time", columnList = "create_time"),
    @Index(name = "idx_submit_time", columnList = "submit_time"),
    @Index(name = "create_by", columnList = "create_by"),
    @Index(name = "update_by", columnList = "update_by"),
    @Index(name = "submit_by", columnList = "submit_by")
})
public class StockOrderJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "order_no", nullable = false, length = 64)
    private String orderNo;

    @Column(name = "type", nullable = false, columnDefinition = "tinyint unsigned")
    private Short type;

    @Column(name = "total_items", nullable = false)
    private Integer totalItems;

    @Column(name = "status", nullable = false, columnDefinition = "tinyint unsigned")
    private Short status;

    @Column(name = "remark", length = 255)
    private String remark;

    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    @Column(name = "create_by", nullable = false)
    private Long createBy;

    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;

    @Column(name = "update_by", nullable = false)
    private Long updateBy;

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

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
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
}
