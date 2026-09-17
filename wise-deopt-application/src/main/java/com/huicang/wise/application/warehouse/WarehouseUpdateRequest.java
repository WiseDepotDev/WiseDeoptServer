package com.huicang.wise.application.warehouse;

/**
 * 更新仓库请求
 *
 * @author WiseDepot
 * @version 0.0.1
 * @since 2026-03-14
 */
public class WarehouseUpdateRequest {

    private String warehouseName;

    private String warehouseCode;

    private String description;

    private String address;

    public String getWarehouseName() {
        return warehouseName;
    }

    public void setWarehouseName(String warehouseName) {
        this.warehouseName = warehouseName;
    }

    public String getWarehouseCode() {
        return warehouseCode;
    }

    public void setWarehouseCode(String warehouseCode) {
        this.warehouseCode = warehouseCode;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
