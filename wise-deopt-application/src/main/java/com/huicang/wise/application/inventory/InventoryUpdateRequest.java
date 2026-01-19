package com.huicang.wise.application.inventory;

/**
 * 类功能描述：库存明细更新请求
 *
 * @author xingchentye
 * @date 2026-01-19
 * @modified xingchentye 2026-01-19 定义库存更新字段
 */
public class InventoryUpdateRequest {

    /**
     * 方法功能描述：库位编码
     */
    private String locationCode;

    /**
     * 方法功能描述：库存数量
     */
    private Integer quantity;

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
}

