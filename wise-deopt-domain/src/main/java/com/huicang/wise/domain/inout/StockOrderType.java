package com.huicang.wise.domain.inout;

/**
 * 出入库单类型枚举
 * 定义出入库单据的类型
 *
 * @author WiseDepot
 * @version 0.0.25
 * @since 2026-02-27
 */
public enum StockOrderType {

    /**
     * 入库单
     */
    INBOUND(0, "入库"),

    /**
     * 出库单
     */
    OUTBOUND(1, "出库");

    private final Integer code;

    private final String description;

    StockOrderType(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 获取类型编码
     *
     * @return 类型编码
     */
    public Integer getCode() {
        return code;
    }

    /**
     * 获取类型描述
     *
     * @return 类型描述
     */
    public String getDescription() {
        return description;
    }

    /**
     * 根据编码获取枚举
     *
     * @param code 类型编码
     * @return 出入库单类型枚举
     */
    public static StockOrderType fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (StockOrderType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }
}
