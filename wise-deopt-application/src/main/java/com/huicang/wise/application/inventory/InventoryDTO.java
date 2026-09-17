package com.huicang.wise.application.inventory;

import java.time.LocalDateTime;

/**
 * 类功能描述：库存明细响应对象
 *
 * @author xingchentye
 * @date 2026-01-19
 * @modified xingchentye 2026-01-19 定义库存响应字段
 */
public class InventoryDTO {

    /**
     * 方法功能描述：库存明细ID
     */
    private Long inventoryId;

    /**
     * 方法功能描述：产品主键ID
     */
    private Long productId;

    /**
     * 方法功能描述：产品名称
     */
    private String productName;

    /**
     * 方法功能描述：产品编码
     */
    private String productCode;

    /**
     * 方法功能描述：产品类型
     */
    private String productType;

    /**
     * 方法功能描述：产品规格
     */
    private String productSpecification;

    /**
     * 方法功能描述：产品单位
     */
    private String productUnit;

    /**
     * 方法功能描述：库存数量
     */
    private Integer quantity;

    /**
     * 方法功能描述：锁定库存数量
     */
    private Integer lockedQuantity;

    /**
     * 方法功能描述：最后盘点时间
     */
    private LocalDateTime lastCheckTime;

    /**
     * 方法功能描述：更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 仓库ID
     */
    private Long warehouseId;

    /**
     * 仓库名称
     */
    private String warehouseName;

    public Long getInventoryId() {
        return inventoryId;
    }

    public void setInventoryId(Long inventoryId) {
        this.inventoryId = inventoryId;
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

    public String getProductCode() {
        return productCode;
    }

    public void setProductCode(String productCode) {
        this.productCode = productCode;
    }

    public String getProductType() {
        return productType;
    }

    public void setProductType(String productType) {
        this.productType = productType;
    }

    public String getProductSpecification() {
        return productSpecification;
    }

    public void setProductSpecification(String productSpecification) {
        this.productSpecification = productSpecification;
    }

    public String getProductUnit() {
        return productUnit;
    }

    public void setProductUnit(String productUnit) {
        this.productUnit = productUnit;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Integer getLockedQuantity() {
        return lockedQuantity;
    }

    public void setLockedQuantity(Integer lockedQuantity) {
        this.lockedQuantity = lockedQuantity;
    }

    public LocalDateTime getLastCheckTime() {
        return lastCheckTime;
    }

    public void setLastCheckTime(LocalDateTime lastCheckTime) {
        this.lastCheckTime = lastCheckTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
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
}


