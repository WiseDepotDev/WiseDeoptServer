package com.huicang.wise.domain.inventory;

import java.time.LocalDateTime;

/**
 * 类功能描述：产品基本信息实体
 *
 * @author xingchentye
 * @date 2026-01-19
 * @modified xingchentye 2026-01-19 定义产品核心字段
 */
public class Product {

    /**
     * 方法功能描述：产品主键ID
     */
    private Long productId;

    /**
     * 方法功能描述：产品编码
     */
    private String productCode;

    /**
     * 方法功能描述：产品名称
     */
    private String productName;

    /**
     * 方法功能描述：规格型号
     */
    private String model;

    /**
     * 方法功能描述：创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 方法功能描述：最后更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 方法功能描述：获取产品主键ID
     *
     * @return 产品主键ID
     */
    public Long getProductId() {
        return productId;
    }

    /**
     * 方法功能描述：设置产品主键ID
     *
     * @param productId 产品主键ID
     * @return 无
     */
    public void setProductId(Long productId) {
        this.productId = productId;
    }

    /**
     * 方法功能描述：获取产品编码
     *
     * @return 产品编码
     */
    public String getProductCode() {
        return productCode;
    }

    /**
     * 方法功能描述：设置产品编码
     *
     * @param productCode 产品编码
     * @return 无
     */
    public void setProductCode(String productCode) {
        this.productCode = productCode;
    }

    /**
     * 方法功能描述：获取产品名称
     *
     * @return 产品名称
     */
    public String getProductName() {
        return productName;
    }

    /**
     * 方法功能描述：设置产品名称
     *
     * @param productName 产品名称
     * @return 无
     */
    public void setProductName(String productName) {
        this.productName = productName;
    }

    /**
     * 方法功能描述：获取规格型号
     *
     * @return 规格型号
     */
    public String getModel() {
        return model;
    }

    /**
     * 方法功能描述：设置规格型号
     *
     * @param model 规格型号
     * @return 无
     */
    public void setModel(String model) {
        this.model = model;
    }

    /**
     * 方法功能描述：获取创建时间
     *
     * @return 创建时间
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * 方法功能描述：设置创建时间
     *
     * @param createdAt 创建时间
     * @return 无
     */
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * 方法功能描述：获取最后更新时间
     *
     * @return 最后更新时间
     */
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /**
     * 方法功能描述：设置最后更新时间
     *
     * @param updatedAt 最后更新时间
     * @return 无
     */
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
