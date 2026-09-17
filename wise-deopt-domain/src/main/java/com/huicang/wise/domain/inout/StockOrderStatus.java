package com.huicang.wise.domain.inout;

/**
 * 出入库单状态枚举
 * 定义出入库单据的状态
 *
 * @author WiseDepot
 * @version 0.0.25
 * @since 2026-02-27
 */
public enum StockOrderStatus {

    /**
     * 待审批
     */
    PENDING(0, "待审批"),

    /**
     * 已审批
     */
    APPROVED(1, "已审批"),

    /**
     * 已完成
     */
    COMPLETED(2, "已完成"),

    /**
     * 已取消
     */
    CANCELLED(3, "已取消"),

    /**
     * 待审核
     */
    SUBMITTED(4, "待审核"),

    /**
     * 已驳回
     */
    REJECTED(5, "已驳回");

    private final Integer code;

    private final String description;

    StockOrderStatus(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 获取状态编码
     *
     * @return 状态编码
     */
    public Integer getCode() {
        return code;
    }

    /**
     * 获取状态描述
     *
     * @return 状态描述
     */
    public String getDescription() {
        return description;
    }

    /**
     * 根据编码获取枚举
     *
     * @param code 状态编码
     * @return 出入库单状态枚举
     */
    public static StockOrderStatus fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (StockOrderStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }
}
