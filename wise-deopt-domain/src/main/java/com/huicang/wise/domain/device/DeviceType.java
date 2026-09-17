package com.huicang.wise.domain.device;

/**
 * 设备类型枚举
 * 定义设备类型
 *
 * @author WiseDepot
 * @version 0.0.23
 * @since 2026-02-27
 */
public enum DeviceType {

    /**
     * RFID读写器
     */
    RFID_READER(0, "RFID读写器"),

    /**
     * 摄像头
     */
    CAMERA(1, "摄像头"),

    /**
     * 巡检小车
     */
    INSPECTION_ROBOT(2, "巡检小车");

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
     * @param code 类型编码
     * @param description 类型描述
     */
    DeviceType(Integer code, String description) {
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
     * 根据编码获取枚举值
     *
     * @param code 类型编码
     * @return 设备类型枚举
     */
    public static DeviceType fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (DeviceType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return null;
    }
}
