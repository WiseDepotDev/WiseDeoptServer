package com.huicang.wise.domain.tag;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * 产品标签实体 存储产品的RFID/条码标签信息
 *
 * @author WiseDepot
 * @version 0.0.23
 * @since 2026-03-03
 */
public class ProductTag {

    /** 标签主键ID */
    private Long tagId;

    /** 关联的产品ID */
    private Long productId;

    /** 条形码 */
    @Size(max = 100, message = "条形码长度不能超过100个字符")
    private String barcode;

    /** NFC标识 */
    @Size(max = 100, message = "NFC标识长度不能超过100个字符")
    private String nfcUid;

    /** RFID标识 */
    @Size(max = 100, message = "RFID标识长度不能超过100个字符")
    private String rfid;

    /** 标签状态：0：未入库 1：已入库 2：已出库 */
    @NotNull(message = "标签状态不能为空")
    private Short status = 0;

    /** 创建者ID */
    @NotNull(message = "创建者ID不能为空")
    private Long createBy;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /**
     * 获取标签主键ID
     *
     * @return 标签主键ID
     */
    public Long getTagId() {
        return tagId;
    }

    /**
     * 设置标签主键ID
     *
     * @param tagId 标签主键ID
     */
    public void setTagId(Long tagId) {
        this.tagId = tagId;
    }

    /**
     * 获取关联的产品ID
     *
     * @return 产品ID
     */
    public Long getProductId() {
        return productId;
    }

    /**
     * 设置关联的产品ID
     *
     * @param productId 产品ID
     */
    public void setProductId(Long productId) {
        this.productId = productId;
    }

    /**
     * 获取条形码
     *
     * @return 条形码
     */
    public String getBarcode() {
        return barcode;
    }

    /**
     * 设置条形码
     *
     * @param barcode 条形码
     */
    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    /**
     * 获取NFC标识
     *
     * @return NFC标识
     */
    public String getNfcUid() {
        return nfcUid;
    }

    /**
     * 设置NFC标识
     *
     * @param nfcUid NFC标识
     */
    public void setNfcUid(String nfcUid) {
        this.nfcUid = nfcUid;
    }

    /**
     * 获取RFID标识
     *
     * @return RFID标识
     */
    public String getRfid() {
        return rfid;
    }

    /**
     * 设置RFID标识
     *
     * @param rfid RFID标识
     */
    public void setRfid(String rfid) {
        this.rfid = rfid;
    }

    /**
     * 获取标签状态
     *
     * @return 标签状态
     */
    public Short getStatus() {
        return status;
    }

    /**
     * 设置标签状态
     *
     * @param status 标签状态
     */
    public void setStatus(Short status) {
        this.status = status;
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
