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
     * 方法功能描述：库位编码
     */
    private String locationCode;

    /**
     * 方法功能描述：库存数量
     */
    private Integer quantity;

    /**
     * 方法功能描述：最后盘点时间
     */
    private LocalDateTime lastCheckTime;

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

    public String getLocationCode() {
        return locationCode;
    }

    public void setLocationCode(String locationCode) {
        this.locationCode = locationCode;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public LocalDateTime getLastCheckTime() {
        return lastCheckTime;
    }

    public void setLastCheckTime(LocalDateTime lastCheckTime) {
        this.lastCheckTime = lastCheckTime;
    }
}

