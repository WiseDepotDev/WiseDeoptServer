package com.huicang.wise.infrastructure.datasource;

/**
 * 数据库上下文持有者
 * 使用ThreadLocal存储当前线程使用的数据源类型
 *
 * @author WiseDepot
 * @version 0.0.21
 * @since 2026-02-27
 */
public final class DatabaseContextHolder {

    private static final ThreadLocal<DatabaseType> CONTEXT_HOLDER = new ThreadLocal<>();

    private DatabaseContextHolder() {
    }

    /**
     * 设置当前线程的数据源类型
     *
     * @param databaseType 数据库类型
     */
    public static void setDatabaseType(DatabaseType databaseType) {
        CONTEXT_HOLDER.set(databaseType);
    }

    /**
     * 获取当前线程的数据源类型
     *
     * @return 数据库类型，如果未设置则返回null
     */
    public static DatabaseType getDatabaseType() {
        return CONTEXT_HOLDER.get();
    }

    /**
     * 清除当前线程的数据源类型
     */
    public static void clearDatabaseType() {
        CONTEXT_HOLDER.remove();
    }

    /**
     * 判断当前线程是否设置了数据源类型
     *
     * @return true表示已设置
     */
    public static boolean hasDatabaseType() {
        return CONTEXT_HOLDER.get() != null;
    }
}
