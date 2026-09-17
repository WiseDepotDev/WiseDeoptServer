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
    USER_CHANGE_PASSWORD("0x1106", "修改密码"),
    USER_CURRENT("0x1107", "获取当前用户"),

    // Device (0x1200 - 0x12FF)
    DEVICE_LIST("0x1201", "获取设备列表"),
    DEVICE_DETAIL("0x1202", "获取设备详情"),
    DEVICE_HEARTBEAT("0x1203", "设备心跳"),
    DEVICE_CREATE("0x1204", "创建设备"),
    DEVICE_UPDATE("0x1205", "更新设备"),
    DEVICE_DELETE("0x1206", "删除设备"),
    DEVICE_STATISTICS("0x1207", "设备统计"),
    DEVICE_CONFIG("0x1208", "获取设备配置"),
    DEVICE_LOG_UPLOAD("0x1209", "上传设备日志"),

    // Inventory (0x1300 - 0x13FF)
    INVENTORY_LIST("0x1301", "获取库存列表"),
    INVENTORY_LIST_ALL("0x1310", "获取全部库存列表"),
    PRODUCT_LIST("0x1302", "获取产品列表"),
    PRODUCT_CREATE("0x1303", "创建产品"),
    PRODUCT_UPDATE("0x1304", "更新产品"),
    PRODUCT_DETAIL("0x1305", "获取产品详情"),
    PRODUCT_DELETE("0x1311", "删除产品"),
    INVENTORY_CREATE("0x1306", "创建库存记录"),
    INVENTORY_UPDATE("0x1307", "更新库存记录"),
    INVENTORY_DETAIL("0x1315", "获取库存详情"),
    INVENTORY_DELETE("0x1316", "删除库存记录"),
    INVENTORY_DIFF_LIST("0x1308", "获取差异列表"),
    INVENTORY_DIFF_REVIEW("0x1309", "复核差异"),
    INVENTORY_SEARCH("0x130A", "搜索库存"),
    INVENTORY_EXPIRING("0x130B", "获取即将过期库存"),
    INVENTORY_TOTAL("0x130C", "获取库存总量"),
    INVENTORY_BY_CATEGORY("0x130D", "按分类获取库存"),
    INVENTORY_BY_LOCATION("0x130E", "按库位获取库存"),
    INVENTORY_PRODUCT_STATISTICS("0x130F", "产品库存统计"),
    INVENTORY_LOCK("0x1312", "锁定库存"),
    INVENTORY_UNLOCK("0x1313", "解锁库存"),
    INVENTORY_LOW_STOCK("0x1314", "低库存预警"),

    // InOut (0x1400 - 0x14FF)
    STOCK_IN("0x1401", "入库单"),
    STOCK_OUT("0x1402", "出库单"),
    STOCK_ORDER_CREATE("0x1403", "创建出入库单"),
    STOCK_ORDER_LIST("0x1404", "获取出入库单列表"),
    STOCK_ORDER_SUBMIT("0x1405", "提交出入库单"),
    STOCK_ORDER_DETAIL("0x1406", "获取出入库单详情"),
    STOCK_ORDER_BIND_LOCATION("0x1407", "入库单绑定货位"),

    // Warehouse (0x1B00 - 0x1BFF)
    WAREHOUSE_ORDER_CREATE("0x1B01", "创建仓库单"),
    WAREHOUSE_ORDER_UPDATE("0x1B02", "更新仓库单"),
    WAREHOUSE_ORDER_CANCEL("0x1B03", "取消仓库单"),
    WAREHOUSE_ORDER_APPROVE("0x1B04", "审批仓库单"),
    WAREHOUSE_ORDER_COMPLETE("0x1B05", "完成仓库单"),
    WAREHOUSE_ORDER_DETAIL("0x1B06", "获取仓库单详情"),
    WAREHOUSE_ORDER_LIST("0x1B07", "获取仓库单列表"),
    WAREHOUSE_ORDER_DETAIL_LIST("0x1B08", "获取仓库单明细列表"),
    WAREHOUSE_ORDER_DETAIL_ADD("0x1B09", "添加仓库单明细"),
    WAREHOUSE_ORDER_DETAIL_DELETE("0x1B0A", "删除仓库单明细"),

    // Task (0x1500 - 0x15FF)
    TASK_LIST("0x1501", "任务列表"),
    TASK_CREATE("0x1502", "创建任务"),
    TASK_UPDATE("0x1503", "更新任务"),
    TASK_DETAIL("0x1504", "获取任务详情"),
    TASK_DELETE("0x1505", "删除任务"),
    TASK_START("0x1506", "开始任务"),
    TASK_PAUSE("0x1507", "暂停任务"),
    TASK_COMPLETE("0x1508", "完成任务"),

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
    TAG_DELETE("0x1906", "删除标签"),
    TAG_BIND("0x1907", "绑定标签"),
    TAG_UNBIND("0x1908", "解绑标签"),
    TAG_BATCH_UNBIND("0x1909", "批量解绑标签"),

    // Scan (0x1A00 - 0x1AFF)
    SCAN_RFID("0x1A01", "RFID扫描"),
    SCAN_BARCODE("0x1A02", "条码扫描"),
    SCAN_RESULT("0x1A03", "获取扫描结果"),
    BATCH_SCAN_CREATE("0x1A04", "创建批量扫描会话"),
    BATCH_SCAN_ADD("0x1A05", "添加扫描到批量会话"),
    BATCH_SCAN_RESULT("0x1A06", "获取批量扫描结果"),
    BATCH_SCAN_DELETE("0x1A07", "删除批量扫描会话"),

    // Report (0x2000 - 0x20FF)
    REPORT_LEDGER("0x2001", "库存台账"),
    REPORT_RECONCILIATION("0x2002", "对账报表"),

    // Common/System
    SYSTEM_ERROR("0x9999", "系统错误"),
    UNKNOWN("0x0000", "未知类型"),
    RFID_DATA_UPLOAD("0x0601", "RFID数据上报");

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
