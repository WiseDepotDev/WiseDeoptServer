package com.huicang.wise.application.alert;

/**
 * 类功能描述：违规移动告警请求
 *
 * @author WiseDepot
 * @version 0.1.17
 * @since 2026-02-27
 */
public class UnauthorizedMoveRequest {

    private Long productId;
    private String productName;
    private String fromLocation;
    private String toLocation;
    private String reason;

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

    public String getFromLocation() {
        return fromLocation;
    }

    public void setFromLocation(String fromLocation) {
        this.fromLocation = fromLocation;
    }

    public String getToLocation() {
        return toLocation;
    }

    public void setToLocation(String toLocation) {
        this.toLocation = toLocation;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
