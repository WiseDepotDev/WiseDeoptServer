package com.huicang.wise.application.inventory;

import java.time.LocalDateTime;

/**
 * 类功能描述：产品信息响应对象
 *
 * @author xingchentye
 * @date 2026-01-19
 * @modified xingchentye 2026-01-19 定义产品响应字段
 * @modified xingchentye 2026-02-27 添加unit、categoryId、description、enabled字段
 */
public class ProductDTO {

    /** 方法功能描述：产品主键ID */
    private Long productId;

    /** 方法功能描述：产品编码 */
    private String productCode;

    /** 方法功能描述：产品名称 */
    private String productName;

    /** 方法功能描述：规格型号 */
    private String model;

    /** 方法功能描述：计量单位 */
    private String unit;

    /** 方法功能描述：产品分类ID */
    private Long categoryId;

    /** 方法功能描述：产品描述 */
    private String description;

    /** 方法功能描述：是否启用 */
    private Boolean enabled;

    /** 方法功能描述：创建时间 */
    private LocalDateTime createdAt;

    /** 方法功能描述：最后更新时间 */
    private LocalDateTime updatedAt;

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getProductCode() {
        return productCode;
    }

    public void setProductCode(String productCode) {
        this.productCode = productCode;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
