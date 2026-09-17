package com.huicang.wise.domain.device;

/**
 * 设备状态枚举
 * 定义设备在线状态
 *
 * @author WiseDepot
 * @version 0.0.23
 * @since 2026-02-27
 */
public enum DeviceStatus {

    /**
     * 离线
     */
    OFFLINE(0, "离线"),

    /**
     * 在线
     */
    ONLINE(1, "在线"),

    /**
     * 故障
     */
    FAULT(2, "故障");

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
     * @param code 状态编码
     * @param description 状态描述
     */
    DeviceStatus(Integer code, String description) {
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
     * 根据编码获取枚举值
     *
     * @param code 状态编码
     * @return 设备状态枚举
     */
    public static DeviceStatus fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (DeviceStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return null;
    }
}
