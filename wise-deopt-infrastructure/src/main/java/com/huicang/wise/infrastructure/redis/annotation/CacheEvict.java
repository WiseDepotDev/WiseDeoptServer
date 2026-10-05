package com.huicang.wise.infrastructure.redis.annotation;

import java.lang.annotation.*;

/**
 * 缓存清除注解 用于标记需要清除缓存的方法，方法执行后将清除指定的缓存
 *
 * <p>**可重复**：一个方法常常要同时作废**两类**缓存。最典型的例子是"删除角色"—— 它既让 `role:<id>` 失效，也让所有靠它拿权限的人的
 * `auth:permission:*` 失效。 不支持重复写的话，第二类只能退化成"在方法体里手动调 `RedisCacheUtils`"，
 * 而**服务定位器式的静态调用会把单元测试一起拖下水**（没有容器就是 NPE）。
 *
 * @author WiseDepot
 * @version 0.0.27
 * @since 2026-03-21
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Repeatable(CacheEvicts.class)
public @interface CacheEvict {

    /**
     * 缓存键前缀
     *
     * @return 缓存键前缀
     */
    String prefix() default "";

    /**
     * 缓存键表达式，支持SpEL表达式 例如：#userId 表示使用方法参数userId作为缓存键
     *
     * @return 缓存键表达式
     */
    String key() default "";

    /**
     * 是否清除所有缓存 true表示清除所有缓存，false表示清除指定缓存
     *
     * @return 是否清除所有缓存
     */
    boolean allEntries() default false;

    /**
     * 是否在方法执行前清除缓存 true表示在方法执行前清除缓存，false表示在方法执行后清除缓存
     *
     * @return 是否在方法执行前清除缓存
     */
    boolean beforeInvocation() default false;

    /**
     * 是否使用哈希结构存储 true表示使用哈希结构，false表示使用字符串结构
     *
     * @return 是否使用哈希结构
     */
    boolean hash() default false;

    /**
     * 哈希字段表达式，支持SpEL表达式 当hash=true时有效
     *
     * @return 哈希字段表达式
     */
    String field() default "";

    /**
     * 条件表达式，支持SpEL表达式 当条件满足时才进行缓存清除
     *
     * @return 条件表达式
     */
    String condition() default "";
}
