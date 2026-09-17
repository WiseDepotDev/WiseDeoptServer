package com.huicang.wise.infrastructure.security;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    int ipLimit() default 100;

    int ipWindowSeconds() default 60;

    int userLimit() default 50;

    int userWindowSeconds() default 60;

    int apiLimit() default 200;

    int apiWindowSeconds() default 60;
}
