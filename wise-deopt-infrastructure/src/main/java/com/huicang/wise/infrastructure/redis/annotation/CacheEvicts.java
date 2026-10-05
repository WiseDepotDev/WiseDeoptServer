package com.huicang.wise.infrastructure.redis.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@link CacheEvict} 的容器注解（Java 语言要求：可重复注解必须有一个容器）。
 *
 * <p>它**不会被业务代码直接使用** —— 写两个 {@code @CacheEvict} 就够了，编译器会自动包一层。 存在的意义是让 `RedisCacheAspect`
 * 有一个地方去读"这个方法要作废哪几类缓存"。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CacheEvicts {

    CacheEvict[] value();
}
