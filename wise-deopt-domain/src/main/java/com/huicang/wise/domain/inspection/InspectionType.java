package com.huicang.wise.domain.inspection;

/**
 * 巡检任务类型枚举
 * 定义巡检任务的创建方式
 *
 * @author WiseDepot
 * @version 0.0.24
 * @since 2026-02-27
 */
public enum InspectionType {

    /**
     * 定时任务
     */
    SCHEDULED(0, "定时任务"),

    /**
     * 手动任务
     */
    MANUAL(1, "手动任务");

    /**
     * 类型编码
     */
    private final Integer code;

    /**
     * 类型描述
     */
    private final String description;

    /**
     * 构造函数
     *
     * @param code        类型编码
     * @param description 类型描述
     */
    InspectionType(Integer code, String description) {
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
     * 根据编码获取枚举实例
     *
     * @param code 类型编码
     * @return 枚举实例，如果未找到则返回null
     */
    public static InspectionType fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (InspectionType type : InspectionType.values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }
}
