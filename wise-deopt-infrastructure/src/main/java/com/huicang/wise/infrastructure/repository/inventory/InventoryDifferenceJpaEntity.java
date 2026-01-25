package com.huicang.wise.infrastructure.repository.inventory;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * 类功能描述：库存差异明细JPA实体
 *
 * @author xingchentye
 * @date 2026-01-25
 */
@Entity
@Table(name = "inventory_difference")
public class InventoryDifferenceJpaEntity {

    /**
     * 差异记录ID
     */
    @Id
    @Column(name = "diff_id")
    private Long diffId;

    /**
     * 关联任务ID
     */
    @Column(name = "task_id")
    private Long taskId;

    /**
     * 产品ID
     */
    @Column(name = "product_id")
    private Long productId;

    /**
     * 产品名称（快照）
     */
    @Column(name = "product_name")
    private String productName;

    /**
     * 库位编码
     */
    @Column(name = "location_code")
    private String locationCode;

    /**
     * 预期数量（系统库存）
     */
    @Column(name = "expected_quantity")
    private Integer expectedQuantity;

    /**
     * 实际数量（盘点结果）
     */
    @Column(name = "actual_quantity")
    private Integer actualQuantity;

    /**
     * 差异类型：MISSING(缺失), SURPLUS(多余), DISPLACED(移位)
     */
    @Column(name = "diff_type")
    private String diffType;

    /**
     * 状态：0-待处理, 1-已处理
     */
    @Column(name = "status")
    private Integer status;

    /**
     * 创建时间
     */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Long getDiffId() {
        return diffId;
    }

    public void setDiffId(Long diffId) {
        this.diffId = diffId;
    }

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getLocationCode() {
        return locationCode;
    }

    public void setLocationCode(String locationCode) {
        this.locationCode = locationCode;
    }

    public Integer getExpectedQuantity() {
        return expectedQuantity;
    }

    public void setExpectedQuantity(Integer expectedQuantity) {
        this.expectedQuantity = expectedQuantity;
    }

    public Integer getActualQuantity() {
        return actualQuantity;
    }

    public void setActualQuantity(Integer actualQuantity) {
        this.actualQuantity = actualQuantity;
    }

    public String getDiffType() {
        return diffType;
    }

    public void setDiffType(String diffType) {
        this.diffType = diffType;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
