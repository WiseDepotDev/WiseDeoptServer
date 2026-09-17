package com.huicang.wise.domain.inout;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * 出入库单明细实体
 * 对应stock_order_item表，存储出入库单据明细信息
 *
 * @author WiseDepot
 * @version 0.0.23
 * @since 2026-03-03
 */
@Entity
@Table(name = "stock_order_item", indexes = {
    @Index(name = "idx_order_id", columnList = "order_id"),
    @Index(name = "idx_tag_id", columnList = "tag_id"),
    @Index(name = "idx_product_id", columnList = "product_id"),
    @Index(name = "idx_create_time", columnList = "create_time")
})
public class StockOrderItem {

    /**
     * 出入库单明细主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "item_id")
    private Long itemId;

    /**
     * 关联的出入库单ID
     */
    @NotNull(message = "出入库单ID不能为空")
    @Column(name = "order_id", nullable = false)
    private Long orderId;

    /**
     * 标签ID
     */
    @Column(name = "tag_id", nullable = true)
    private Long tagId;

    /**
     * 产品ID
     */
    @NotNull(message = "产品ID不能为空")
    @Column(name = "product_id", nullable = false)
    private Long productId;

    /**
     * 数量
     */
    @Column(name = "quantity", nullable = true)
    private Integer quantity;

    /**
     * 库位编码
     */
    @Column(name = "location_code", nullable = true)
    private String locationCode;

    /**
     * 创建者ID
     */
    @NotNull(message = "创建者ID不能为空")
    @Column(name = "create_by", nullable = false)
    private Long createBy;

    /**
     * 创建时间
     */
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    /**
     * 获取出入库单明细主键ID
     *
     * @return 出入库单明细主键ID
     */
    public Long getItemId() {
        return itemId;
    }

    /**
     * 设置出入库单明细主键ID
     *
     * @param itemId 出入库单明细主键ID
     */
    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    /**
     * 获取关联的出入库单ID
     *
     * @return 关联的出入库单ID
     */
    public Long getOrderId() {
        return orderId;
    }

    /**
     * 设置关联的出入库单ID
     *
     * @param orderId 关联的出入库单ID
     */
    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    /**
     * 获取标签ID
     *
     * @return 标签ID
     */
    public Long getTagId() {
        return tagId;
    }

    /**
     * 设置标签ID
     *
     * @param tagId 标签ID
     */
    public void setTagId(Long tagId) {
        this.tagId = tagId;
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
     * 获取数量
     *
     * @return 数量
     */
    public Integer getQuantity() {
        return quantity;
    }

    /**
     * 设置数量
     *
     * @param quantity 数量
     */
    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    /**
     * 获取库位编码
     *
     * @return 库位编码
     */
    public String getLocationCode() {
        return locationCode;
    }

    /**
     * 设置库位编码
     *
     * @param locationCode 库位编码
     */
    public void setLocationCode(String locationCode) {
        this.locationCode = locationCode;
    }

    /**
     * 获取创建者ID
     *
     * @return 创建者ID
     */
    public Long getCreateBy() {
        return createBy;
    }

    /**
     * 设置创建者ID
     *
     * @param createBy 创建者ID
     */
    public void setCreateBy(Long createBy) {
        this.createBy = createBy;
    }

    /**
     * 获取创建时间
     *
     * @return 创建时间
     */
    public LocalDateTime getCreateTime() {
        return createTime;
    }

    /**
     * 设置创建时间
     *
     * @param createTime 创建时间
     */
    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
