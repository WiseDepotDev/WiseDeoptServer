package com.huicang.wise.domain.inout;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "stock_order_item", indexes = {
    @Index(name = "idx_order_id", columnList = "order_id"),
    @Index(name = "idx_tag_id", columnList = "tag_id"),
    @Index(name = "idx_product_id", columnList = "product_id"),
    @Index(name = "idx_create_time", columnList = "create_time"),
    @Index(name = "create_by", columnList = "create_by")
})
public class StockOrderDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "item_id")
    private Long itemId;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "tag_id", nullable = true)
    private Long tagId;

    @Column(name = "quantity", nullable = true)
    private Integer quantity;

    @Column(name = "location_code", nullable = true)
    private String locationCode;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    @Column(name = "create_by", nullable = false)
    private Long createBy;

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Long getTagId() {
        return tagId;
    }

    public void setTagId(Long tagId) {
        this.tagId = tagId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getLocationCode() {
        return locationCode;
    }

    public void setLocationCode(String locationCode) {
        this.locationCode = locationCode;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
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
}
