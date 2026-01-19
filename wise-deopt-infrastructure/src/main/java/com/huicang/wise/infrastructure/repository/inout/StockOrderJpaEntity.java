package com.huicang.wise.infrastructure.repository.inout;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * 类功能描述：出入库单JPA实体
 *
 * @author xingchentye
 * @date 2026-01-19
 * @modified xingchentye 2026-01-19 映射stock_order表
 */
@Entity
@Table(name = "stock_order")
public class StockOrderJpaEntity {

    @Id
    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "order_no")
    private String orderNo;

    @Column(name = "order_type")
    private String orderType;

    @Column(name = "order_status")
    private String orderStatus;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /**
     * 方法功能描述：获取出入库单ID
     *
     * @return 出入库单ID
     */
    public Long getOrderId() {
        return orderId;
    }

    /**
     * 方法功能描述：设置出入库单ID
     *
     * @param orderId 出入库单ID
     * @return 无
     */
    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    /**
     * 方法功能描述：获取出入库单号
     *
     * @return 出入库单号
     */
    public String getOrderNo() {
        return orderNo;
    }

    /**
     * 方法功能描述：设置出入库单号
     *
     * @param orderNo 出入库单号
     * @return 无
     */
    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    /**
     * 方法功能描述：获取单据类型
     *
     * @return 单据类型
     */
    public String getOrderType() {
        return orderType;
    }

    /**
     * 方法功能描述：设置单据类型
     *
     * @param orderType 单据类型
     * @return 无
     */
    public void setOrderType(String orderType) {
        this.orderType = orderType;
    }

    /**
     * 方法功能描述：获取单据状态
     *
     * @return 单据状态
     */
    public String getOrderStatus() {
        return orderStatus;
    }

    /**
     * 方法功能描述：设置单据状态
     *
     * @param orderStatus 单据状态
     * @return 无
     */
    public void setOrderStatus(String orderStatus) {
        this.orderStatus = orderStatus;
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
}

