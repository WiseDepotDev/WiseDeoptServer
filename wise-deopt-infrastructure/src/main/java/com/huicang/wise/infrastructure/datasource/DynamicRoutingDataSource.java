package com.huicang.wise.infrastructure.datasource;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

/**
 * 动态路由数据源
 * 根据当前线程的数据库上下文选择对应的数据源
 *
 * @author WiseDepot
 * @version 0.0.21
 * @since 2026-02-27
 */
public class DynamicRoutingDataSource extends AbstractRoutingDataSource {

    /**
     * 确定当前查找键
     * 返回当前线程设置的数据库类型作为数据源查找键
     *
     * @return 当前数据库类型
     */
    @Override
    protected Object determineCurrentLookupKey() {
        DatabaseType databaseType = DatabaseContextHolder.getDatabaseType();
        if (databaseType == null) {
            return DatabaseType.USER;
        }
        return databaseType;
    }
}
