package com.huicang.wise.application.inout;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

public class StockOrderItemCreateRequest {
    @NotNull(message = "产品ID不能为空")
    private Long productId;

    @NotNull(message = "数量不能为空")
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
