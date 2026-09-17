package com.huicang.wise.infrastructure.datasource;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * 动态数据源切面
 * 根据@TargetDataSource注解切换数据源
 *
 * @author WiseDepot
 * @version 0.0.21
 * @since 2026-02-27
 */
@Aspect
@Component
@Order(-1)
public class DynamicDataSourceAspect {

    private static final Logger log = LoggerFactory.getLogger(DynamicDataSourceAspect.class);

    /**
     * 环绕通知，处理数据源切换
     *
     * @param joinPoint 切入点
     * @return 方法执行结果
     * @throws Throwable 方法执行异常
     */
    @Around("@annotation(com.huicang.wise.infrastructure.datasource.TargetDataSource) || " +
            "@within(com.huicang.wise.infrastructure.datasource.TargetDataSource)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        TargetDataSource targetDataSource = method.getAnnotation(TargetDataSource.class);
        if (targetDataSource == null) {
            targetDataSource = joinPoint.getTarget().getClass().getAnnotation(TargetDataSource.class);
        }

        DatabaseType previousDatabaseType = DatabaseContextHolder.getDatabaseType();

        if (targetDataSource != null) {
            DatabaseType databaseType = targetDataSource.value();
            DatabaseContextHolder.setDatabaseType(databaseType);
            log.debug("切换数据源到: {}", databaseType.getCode());
        }

        try {
            return joinPoint.proceed();
        } finally {
            if (previousDatabaseType != null) {
                DatabaseContextHolder.setDatabaseType(previousDatabaseType);
                log.debug("恢复数据源到: {}", previousDatabaseType.getCode());
            } else {
                DatabaseContextHolder.clearDatabaseType();
                log.debug("清除数据源上下文");
            }
        }
    }
}
