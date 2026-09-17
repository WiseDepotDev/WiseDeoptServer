package com.huicang.wise.domain.alert;

/**
 * 告警级别枚举
 * 定义告警事件的严重程度级别
 *
 * @author WiseDepot
 * @version 0.0.24
 * @since 2026-02-27
 */
public enum AlertLevel {

    /**
     * 提示级别
     */
    INFO(0, "提示"),

    /**
     * 一般级别
     */
    WARNING(1, "一般"),

    /**
     * 严重级别
     */
    ERROR(2, "严重"),

    /**
     * 紧急级别
     */
    CRITICAL(3, "紧急");

    /**
     * 级别编码
     */
    private final Integer code;

    /**
     * 级别描述
     */
    private final String description;

    /**
     * 构造函数
     *
     * @param code        级别编码
     * @param description 级别描述
     */
    AlertLevel(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 获取级别编码
     *
     * @return 级别编码
     */
    public Integer getCode() {
        return code;
    }

    /**
     * 获取级别描述
     *
     * @return 级别描述
     */
    public String getDescription() {
        return description;
    }

    /**
     * 根据编码获取枚举实例
     *
     * @param code 级别编码
     * @return 枚举实例，如果未找到则返回null
     */
    public static AlertLevel fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (AlertLevel level : AlertLevel.values()) {
            if (level.getCode().equals(code)) {
                return level;
            }
        }
        return null;
    }
}
