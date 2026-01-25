package com.huicang.wise.application.inout;

/**
 * 类功能描述：出入库单明细DTO
 *
 * @author xingchentye
 * @date 2026-01-22
 */
public class StockOrderItemDTO {

    private Long productId;

    private Integer quantity;

    private String locationCode;

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

    public String getLocationCode() {
        return locationCode;
    }

    public void setLocationCode(String locationCode) {
        this.locationCode = locationCode;
    }
}
