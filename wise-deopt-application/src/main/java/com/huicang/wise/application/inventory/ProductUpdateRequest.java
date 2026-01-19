package com.huicang.wise.application.inventory;

/**
 * 类功能描述：产品更新请求
 *
 * @author xingchentye
 * @date 2026-01-19
 * @modified xingchentye 2026-01-19 定义产品更新字段
 */
public class ProductUpdateRequest {

    /**
     * 方法功能描述：产品名称
     */
    private String productName;

    /**
     * 方法功能描述：规格型号
     */
    private String model;

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
}

