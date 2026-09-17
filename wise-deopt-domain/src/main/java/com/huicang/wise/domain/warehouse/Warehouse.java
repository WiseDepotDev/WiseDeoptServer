package com.huicang.wise.domain.warehouse;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * 仓库实体
 *
 * @author WiseDepot
 * @version 0.0.1
 * @since 2026-03-14
 */
@Entity
@Table(name = "warehouse", indexes = {
    @Index(name = "idx_warehouse_name", columnList = "warehouse_name"),
    @Index(name = "idx_warehouse_code", columnList = "warehouse_code", unique = true)
})
public class Warehouse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "warehouse_id")
    private Long warehouseId;

    @NotNull(message = "仓库名称不能为空")
    @Column(name = "warehouse_name", nullable = false)
    private String warehouseName;

    @NotNull(message = "仓库编码不能为空")
    @Column(name = "warehouse_code", nullable = false)
    private String warehouseCode;

    @Column(name = "description")
    private String description;

    @Column(name = "address")
    private String address;

    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;

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

    public String getWarehouseCode() {
        return warehouseCode;
    }

    public void setWarehouseCode(String warehouseCode) {
        this.warehouseCode = warehouseCode;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}
