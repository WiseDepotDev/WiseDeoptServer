package com.huicang.wise.application.alert;

/**
 * 类功能描述：库存异常告警请求
 *
 * @author WiseDepot
 * @version 0.1.17
 * @since 2026-02-27
 */
public class InventoryAbnormalRequest {

    private Long productId;
    private String productName;
    private Integer expectedQuantity;
    private Integer actualQuantity;
    private String warehouseName;

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

    public String getWarehouseName() {
        return warehouseName;
    }

    public void setWarehouseName(String warehouseName) {
        this.warehouseName = warehouseName;
    }
}