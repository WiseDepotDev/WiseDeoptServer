package com.huicang.wise.domain.alert;

/**
 * 告警状态枚举
 * 定义告警事件的处理状态
 *
 * @author WiseDepot
 * @version 0.0.24
 * @since 2026-02-27
 */
public enum AlertStatus {

    /**
     * 未处理
     */
    PENDING(0, "未处理"),

    /**
     * 处理中
     */
    PROCESSING(1, "处理中"),

    /**
     * 已处理
     */
    RESOLVED(2, "已处理"),

    /**
     * 已忽略
     */
    IGNORED(3, "已忽略");

    /**
     * 状态编码
     */
    private final Integer code;

    /**
     * 状态描述
     */
    private final String description;

    /**
     * 构造函数
     *
     * @param code        状态编码
     * @param description 状态描述
     */
    AlertStatus(Integer code, String description) {
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
     * 根据编码获取枚举实例
     *
     * @param code 状态编码
     * @return 枚举实例，如果未找到则返回null
     */
    public static AlertStatus fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (AlertStatus status : AlertStatus.values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }
}
