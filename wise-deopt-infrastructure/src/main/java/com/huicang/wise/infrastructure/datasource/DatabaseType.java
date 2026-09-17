package com.huicang.wise.infrastructure.datasource;

/**
 * 数据库类型枚举
 * 定义系统中使用的所有逻辑数据库
 *
 * @author WiseDepot
 * @version 0.0.21
 * @since 2026-02-27
 */
public enum DatabaseType {

    USER("user", "用户服务数据库"),

    AUTH("auth", "身份认证与授权服务数据库"),

    INVENTORY("inventory", "库存服务数据库"),

    TAG("tag", "标签服务数据库"),

    INOUT("inout", "出入库服务数据库"),

    DEVICE("device", "设备服务数据库"),

    MONITOR("monitor", "监控服务数据库"),

    INSPECTION("inspection", "巡检服务数据库"),

    REPORT("report", "报表服务数据库"),

    ALERT("alert", "告警服务数据库");

    private final String code;

    private final String description;

    DatabaseType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static DatabaseType fromCode(String code) {
        for (DatabaseType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown database type: " + code);
    }
}
