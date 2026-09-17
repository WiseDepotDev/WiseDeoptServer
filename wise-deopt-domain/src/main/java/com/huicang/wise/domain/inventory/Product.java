package com.huicang.wise.domain.inventory;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * 产品信息实体
 * 存储产品的基本信息
 *
 * @author WiseDepot
 * @version 0.0.23
 * @since 2026-03-03
 */
@Entity
@Table(name = "product", indexes = {
    @Index(name = "idx_name", columnList = "name"),
    @Index(name = "idx_create_time", columnList = "create_time")
})
public class Product {

    /**
     * 产品主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private Long productId;

    /**
     * 产品名称
     */
    @NotBlank(message = "产品名称不能为空")
    @Size(max = 128, message = "产品名称长度不能超过128个字符")
    @Column(name = "name", nullable = false, length = 128)
    private String name;

    /**
     * 产品编码
     */
    @NotBlank(message = "产品编码不能为空")
    @Size(max = 64, message = "产品编码长度不能超过64个字符")
    @Column(name = "code", nullable = false, length = 64)
    private String code;

    /**
     * 规格型号
     */
    @Size(max = 128, message = "规格型号长度不能超过128个字符")
    @Column(name = "model", length = 128)
    private String model;

    /**
     * 计量单位
     */
    @NotBlank(message = "计量单位不能为空")
    @Size(max = 8, message = "计量单位长度不能超过8个字符")
    @Column(name = "unit", nullable = false, length = 8)
    private String unit = "个";

    /**
     * 创建者ID
     */
    @NotNull(message = "创建者ID不能为空")
    @Column(name = "create_by", nullable = false)
    private Long createBy;

    /**
     * 创建时间
     */
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    /**
     * 更新者ID
     */
    @NotNull(message = "更新者ID不能为空")
    @Column(name = "update_by", nullable = false)
    private Long updateBy;

    /**
     * 更新时间
     */
    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;

    /**
     * 获取产品主键ID
     *
     * @return 产品主键ID
     */
    public Long getProductId() {
        return productId;
    }

    /**
     * 设置产品主键ID
     *
     * @param productId 产品主键ID
     */
    public void setProductId(Long productId) {
        this.productId = productId;
    }

    /**
     * 获取产品名称
     *
     * @return 产品名称
     */
    public String getName() {
        return name;
    }

    /**
     * 设置产品名称
     *
     * @param name 产品名称
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * 获取产品编码
     *
     * @return 产品编码
     */
    public String getCode() {
        return code;
    }

    /**
     * 设置产品编码
     *
     * @param code 产品编码
     */
    public void setCode(String code) {
        this.code = code;
    }

    /**
     * 获取规格型号
     *
     * @return 规格型号
     */
    public String getModel() {
        return model;
    }

    /**
     * 设置规格型号
     *
     * @param model 规格型号
     */
    public void setModel(String model) {
        this.model = model;
    }

    /**
     * 获取计量单位
     *
     * @return 计量单位
     */
    public String getUnit() {
        return unit;
    }

    /**
     * 设置计量单位
     *
     * @param unit 计量单位
     */
    public void setUnit(String unit) {
        this.unit = unit;
    }

    /**
     * 获取创建者ID
     *
     * @return 创建者ID
     */
    public Long getCreateBy() {
        return createBy;
    }

    /**
     * 设置创建者ID
     *
     * @param createBy 创建者ID
     */
    public void setCreateBy(Long createBy) {
        this.createBy = createBy;
    }

    /**
     * 获取创建时间
     *
     * @return 创建时间
     */
    public LocalDateTime getCreateTime() {
        return createTime;
    }

    /**
     * 设置创建时间
     *
     * @param createTime 创建时间
     */
    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    /**
     * 获取更新者ID
     *
     * @return 更新者ID
     */
    public Long getUpdateBy() {
        return updateBy;
    }

    /**
     * 设置更新者ID
     *
     * @param updateBy 更新者ID
     */
    public void setUpdateBy(Long updateBy) {
        this.updateBy = updateBy;
    }

    /**
     * 获取更新时间
     *
     * @return 更新时间
     */
    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    /**
     * 设置更新时间
     *
     * @param updateTime 更新时间
     */
    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}
