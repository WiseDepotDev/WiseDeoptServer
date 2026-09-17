package com.huicang.wise.infrastructure.datasource;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 数据源注解
 * 用于标记方法或类使用的数据源类型
 *
 * @author WiseDepot
 * @version 0.0.21
 * @since 2026-02-27
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TargetDataSource {

    /**
     * 数据库类型
     *
     * @return 数据库类型枚举
     */
    DatabaseType value() default DatabaseType.USER;
}
