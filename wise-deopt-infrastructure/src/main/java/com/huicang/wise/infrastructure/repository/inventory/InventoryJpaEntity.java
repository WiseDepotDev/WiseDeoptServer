package com.huicang.wise.infrastructure.repository.inventory;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * 类功能描述：库存明细JPA实体
 *
 * @author xingchentye
 * @date 2026-01-19
 * @modified xingchentye 2026-01-19 映射inventory表
 */
@Entity
@Table(name = "inventory")
public class InventoryJpaEntity {

    @Id
    @Column(name = "inventory_id")
    private Long inventoryId;

    @Column(name = "product_id")
    private Long productId;

    @Column(name = "location_code")
    private String locationCode;

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "last_check_time")
    private LocalDateTime lastCheckTime;

    /**
     * 方法功能描述：获取库存明细ID
     *
     * @return 库存明细ID
     */
    public Long getInventoryId() {
        return inventoryId;
    }

    /**
     * 方法功能描述：设置库存明细ID
     *
     * @param inventoryId 库存明细ID
     * @return 无
     */
    public void setInventoryId(Long inventoryId) {
        this.inventoryId = inventoryId;
    }

    /**
     * 方法功能描述：获取产品主键ID
     *
     * @return 产品主键ID
     */
    public Long getProductId() {
        return productId;
    }

    /**
     * 方法功能描述：设置产品主键ID
     *
     * @param productId 产品主键ID
     * @return 无
     */
    public void setProductId(Long productId) {
        this.productId = productId;
    }

    /**
     * 方法功能描述：获取库位编码
     *
     * @return 库位编码
     */
    public String getLocationCode() {
        return locationCode;
    }

    /**
     * 方法功能描述：设置库位编码
     *
     * @param locationCode 库位编码
     * @return 无
     */
    public void setLocationCode(String locationCode) {
        this.locationCode = locationCode;
    }

    /**
     * 方法功能描述：获取库存数量
     *
     * @return 库存数量
     */
    public Integer getQuantity() {
        return quantity;
    }

    /**
     * 方法功能描述：设置库存数量
     *
     * @param quantity 库存数量
     * @return 无
     */
    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    /**
     * 方法功能描述：获取最后盘点时间
     *
     * @return 最后盘点时间
     */
    public LocalDateTime getLastCheckTime() {
        return lastCheckTime;
    }

    /**
     * 方法功能描述：设置最后盘点时间
     *
     * @param lastCheckTime 最后盘点时间
     * @return 无
     */
    public void setLastCheckTime(LocalDateTime lastCheckTime) {
        this.lastCheckTime = lastCheckTime;
    }
}
