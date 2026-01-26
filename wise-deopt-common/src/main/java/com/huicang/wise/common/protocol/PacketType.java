package com.huicang.wise.common.protocol;

/**
 * 协议类型定义
 * 格式：0x[模块][功能]
 * 10: Auth
 * 11: User
 * 12: Device
 * 13: Inventory
 * 14: InOut
 * 15: Task
 * 16: Alert
 * 17: Dashboard
 * 18: Oss
 * 19: Tag
 * 99: System/Common
 *
 * @author xingchentye
 * @version 1.0
 */
public enum PacketType {
    // Auth (0x1000 - 0x10FF)
    AUTH_LOGIN("0x1001", "用户登录"),
    AUTH_NFC_LOGIN("0x1002", "NFC登录"),
    AUTH_NFC_PIN_LOGIN("0x1003", "NFC+PIN登录"),
    AUTH_LOGOUT("0x1004", "用户登出"),

    // User (0x1100 - 0x11FF)
    USER_LIST("0x1101", "获取用户列表"),
    USER_DETAIL("0x1102", "获取用户详情"),
    USER_CREATE("0x1103", "创建用户"),
    USER_UPDATE("0x1104", "更新用户"),
    USER_DELETE("0x1105", "删除用户"),

    // Device (0x1200 - 0x12FF)
    DEVICE_LIST("0x1201", "获取设备列表"),
    DEVICE_DETAIL("0x1202", "获取设备详情"),
    DEVICE_HEARTBEAT("0x1203", "设备心跳"),
    DEVICE_CREATE("0x1204", "创建设备"),
    DEVICE_UPDATE("0x1205", "更新设备"),
    DEVICE_DELETE("0x1206", "删除设备"),

    // Inventory (0x1300 - 0x13FF)
    INVENTORY_LIST("0x1301", "获取库存列表"),
    PRODUCT_LIST("0x1302", "获取产品列表"),
    PRODUCT_CREATE("0x1303", "创建产品"),
    PRODUCT_UPDATE("0x1304", "更新产品"),
    PRODUCT_DETAIL("0x1305", "获取产品详情"),
    INVENTORY_CREATE("0x1306", "创建库存记录"),
    INVENTORY_UPDATE("0x1307", "更新库存记录"),
    INVENTORY_DIFF_LIST("0x1308", "获取差异列表"),
    INVENTORY_DIFF_REVIEW("0x1309", "复核差异"),

    // InOut (0x1400 - 0x14FF)
    STOCK_IN("0x1401", "入库单"),
    STOCK_OUT("0x1402", "出库单"),
    STOCK_ORDER_CREATE("0x1403", "创建出入库单"),
    STOCK_ORDER_LIST("0x1404", "获取出入库单列表"),
    STOCK_ORDER_SUBMIT("0x1405", "提交出入库单"),
    STOCK_ORDER_DETAIL("0x1406", "获取出入库单详情"),

    // Task (0x1500 - 0x15FF)
    TASK_LIST("0x1501", "任务列表"),
    TASK_CREATE("0x1502", "创建任务"),
    TASK_UPDATE("0x1503", "更新任务"),
    TASK_DETAIL("0x1504", "获取任务详情"),
    TASK_DELETE("0x1505", "删除任务"),

    // Alert (0x1600 - 0x16FF)
    ALERT_LIST("0x1601", "告警列表"),
    ALERT_CREATE("0x1602", "创建告警"),
    ALERT_LIST_BY_LEVEL("0x1603", "按级别查询告警"),
    ALERT_UPDATE("0x1604", "更新告警"),
    ALERT_ACK("0x1605", "确认告警"),

    // Dashboard (0x1700 - 0x17FF)
    DASHBOARD_SUMMARY("0x1701", "首页看板汇总"),

    // Oss (0x1800 - 0x18FF)
    OSS_FILE_CREATE("0x1801", "创建文件记录"),
    OSS_PRESIGNED_URL("0x1802", "生成预签名URL"),

    // Tag (0x1900 - 0x19FF)
    TAG_CREATE("0x1901", "创建标签"),
    TAG_UPDATE("0x1902", "更新标签"),
    TAG_DETAIL("0x1903", "获取标签详情"),
    TAG_LIST("0x1904", "获取标签列表"),
    TAG_BATCH_BIND("0x1905", "批量绑定标签"),

    // Report (0x2000 - 0x20FF)
    REPORT_LEDGER("0x2001", "库存台账"),

    // Common/System
    SYSTEM_ERROR("0x9999", "系统错误"),
    UNKNOWN("0x0000", "未知类型");

    private final String code;
    private final String description;

    PacketType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}
