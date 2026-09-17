package com.huicang.wise.application.inout;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public class StockOrderCreateRequest {
    @NotNull(message = "仓库ID不能为空")
    private Long warehouseId;

    @NotNull(message = "单据编号不能为空")
    private String orderNo;

    @NotNull(message = "单据类型不能为空")
    @JsonProperty("orderType")
    private String type; // 前端传的是 String (IN/OUT)，后端目前是 Short? 需要确认转换逻辑

    @JsonProperty("orderStatus")
    private String status; // 前端传的是 String (PENDING)，后端目前是 Short?

    private String remark;
    
    @NotNull(message = "创建人不能为空")
    private Long createBy;
    
    private Long updateBy;

    private List<StockOrderItemCreateRequest> items;

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public Long getCreateBy() {
        return createBy;
    }

    public void setCreateBy(Long createBy) {
        this.createBy = createBy;
    }

    public Long getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(Long updateBy) {
        this.updateBy = updateBy;
    }

    public List<StockOrderItemCreateRequest> getItems() {
        return items;
    }

    public void setItems(List<StockOrderItemCreateRequest> items) {
        this.items = items;
    }
}
