package com.huicang.wise.infrastructure.redis.annotation;

import java.lang.annotation.*;

/**
 * 缓存注解 用于标记需要缓存的方法，方法执行结果将被缓存
 *
 * @author WiseDepot
 * @version 0.0.27
 * @since 2026-03-21
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Cacheable {

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
     * 缓存过期时间（秒） 默认为3600秒（1小时）
     *
     * @return 缓存过期时间
     */
    long timeout() default 3600;

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
     * 条件表达式，支持SpEL表达式 当条件满足时才进行缓存
     *
     * @return 条件表达式
     */
    String condition() default "";

    /**
     * 排除条件表达式，支持SpEL表达式 当条件满足时不进行缓存
     *
     * @return 排除条件表达式
     */
    String unless() default "";
}
