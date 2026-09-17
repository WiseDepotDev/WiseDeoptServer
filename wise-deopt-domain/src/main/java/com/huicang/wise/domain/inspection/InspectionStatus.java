package com.huicang.wise.domain.inspection;

/**
 * 巡检任务状态枚举
 * 定义巡检任务的执行状态
 *
 * @author WiseDepot
 * @version 0.0.24
 * @since 2026-02-27
 */
public enum InspectionStatus {

    /**
     * 待执行
     */
    PENDING(0, "待执行"),

    /**
     * 执行中
     */
    IN_PROGRESS(1, "执行中"),

    /**
     * 已完成
     */
    COMPLETED(2, "已完成"),

    /**
     * 异常终止
     */
    ABORTED(3, "异常终止");

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
    InspectionStatus(Integer code, String description) {
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
    public static InspectionStatus fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (InspectionStatus status : InspectionStatus.values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }
}
