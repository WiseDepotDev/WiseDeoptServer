package com.huicang.wise.application.inventory;

/**
 * 类功能描述：库存明细创建请求
 *
 * @author xingchentye
 * @date 2026-01-19
 * @modified xingchentye 2026-01-19 定义库存创建字段
 */
public class InventoryCreateRequest {

    /**
     * 方法功能描述：仓库ID
     */
    private Long warehouseId;

    /**
     * 方法功能描述：产品主键ID
     */
    private Long productId;

    /**
     * 方法功能描述：库存数量
     */
    private Integer quantity;

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}

