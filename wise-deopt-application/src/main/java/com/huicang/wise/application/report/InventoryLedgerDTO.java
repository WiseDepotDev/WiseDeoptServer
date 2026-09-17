package com.huicang.wise.application.report;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * 库存台账DTO
 */
@Schema(description = "库存台账信息")
public class InventoryLedgerDTO {

    @Schema(description = "发生时间")
    private LocalDateTime time;

    @Schema(description = "单据编号")
    private String orderNo;

    @Schema(description = "类型(IN/OUT)")
    private String type;

    @Schema(description = "变动数量")
    private Integer quantity;

    @Schema(description = "库位")
    private String locationCode;

    public LocalDateTime getTime() {
        return time;
    }

    public void setTime(LocalDateTime time) {
        this.time = time;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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

