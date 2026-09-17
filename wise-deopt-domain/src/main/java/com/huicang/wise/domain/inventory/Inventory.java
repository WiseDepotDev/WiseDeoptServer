package com.huicang.wise.domain.inventory;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * 库存信息实体
 * 存储产品的库存数量信息
 *
 * @author WiseDepot
 * @version 0.0.23
 * @since 2026-03-03
 */
@Entity
@Table(name = "inventory", indexes = {
    @Index(name = "uk_warehouse_product", columnList = "warehouse_id, product_id", unique = true),
    @Index(name = "idx_update_time", columnList = "update_time")
})
public class Inventory {

    /**
     * 库存主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "inventory_id")
    private Long inventoryId;

    /**
     * 仓库ID
     */
    @NotNull(message = "仓库ID不能为空")
    @Column(name = "warehouse_id", nullable = false)
    private Long warehouseId;

    /**
     * 产品ID
     */
    @NotNull(message = "产品ID不能为空")
    @Column(name = "product_id", nullable = false)
    private Long productId;

    /**
     * 库存总量
     */
    @NotNull(message = "库存总量不能为空")
    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    /**
     * 锁定库存数量
     */
    @NotNull(message = "锁定库存数量不能为空")
    @Column(name = "locked_quantity", nullable = false)
    private Integer lockedQuantity;

    /**
     * 最后库存变动时间
     */
    @NotNull(message = "最后库存变动时间不能为空")
    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;

    /**
     * 获取库存主键ID
     *
     * @return 库存主键ID
     */
    public Long getInventoryId() {
        return inventoryId;
    }

    /**
     * 设置库存主键ID
     *
     * @param inventoryId 库存主键ID
     */
    public void setInventoryId(Long inventoryId) {
        this.inventoryId = inventoryId;
    }

    /**
     * 获取仓库ID
     *
     * @return 仓库ID
     */
    public Long getWarehouseId() {
        return warehouseId;
    }

    /**
     * 设置仓库ID
     *
     * @param warehouseId 仓库ID
     */
    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    /**
     * 获取产品ID
     *
     * @return 产品ID
     */
    public Long getProductId() {
        return productId;
    }

    /**
     * 设置产品ID
     *
     * @param productId 产品ID
     */
    public void setProductId(Long productId) {
        this.productId = productId;
    }

    /**
     * 获取库存总量
     *
     * @return 库存总量
     */
    public Integer getQuantity() {
        return quantity;
    }

    /**
     * 设置库存总量
     *
     * @param quantity 库存总量
     */
    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    /**
     * 获取锁定库存数量
     *
     * @return 锁定库存数量
     */
    public Integer getLockedQuantity() {
        return lockedQuantity;
    }

    /**
     * 设置锁定库存数量
     *
     * @param lockedQuantity 锁定库存数量
     */
    public void setLockedQuantity(Integer lockedQuantity) {
        this.lockedQuantity = lockedQuantity;
    }

    /**
     * 获取最后库存变动时间
     *
     * @return 最后库存变动时间
     */
    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    /**
     * 设置最后库存变动时间
     *
     * @param updateTime 最后库存变动时间
     */
    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}
