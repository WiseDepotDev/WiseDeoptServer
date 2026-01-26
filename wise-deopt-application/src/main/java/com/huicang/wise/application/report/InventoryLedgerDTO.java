package com.huicang.wise.application.report;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.time.LocalDateTime;

/**
 * 库存台账DTO
 */
@ApiModel("库存台账信息")
public class InventoryLedgerDTO {

    @ApiModelProperty("发生时间")
    private LocalDateTime time;

    @ApiModelProperty("单据编号")
    private String orderNo;

    @ApiModelProperty("类型(IN/OUT)")
    private String type;

    @ApiModelProperty("变动数量")
    private Integer quantity;

    @ApiModelProperty("库位")
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
