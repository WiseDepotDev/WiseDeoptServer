package com.huicang.wise.api.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 类功能描述：Web请求日志切面
 *
 * @author xingchentye
 * @date 2026-01-23
 */
@Aspect
@Component
@Slf4j
public class WebLogAspect {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 定义切入点，拦截 controller 包下的所有方法
     */
    @Pointcut("execution(public * com.huicang.wise.api.controller..*.*(..))")
    public void webLog() {
    }

    /**
     * 环绕通知
     *
     * @param joinPoint 切入点
     * @return 方法执行结果
     * @throws Throwable 异常
     */
    @Around("webLog()")
    public Object doAround(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        
        // 获取当前请求对象
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes != null ? attributes.getRequest() : null;

        String url = request != null ? request.getRequestURL().toString() : "N/A";
        String method = request != null ? request.getMethod() : "N/A";
        String ip = request != null ? request.getRemoteAddr() : "N/A";
        String className = joinPoint.getSignature().getDeclaringTypeName();
        String methodName = joinPoint.getSignature().getName();

        // 打印请求日志
        log.info("========================================== Start ==========================================");
        log.info("URL          : {}", url);
        log.info("Method       : {}", method);
        log.info("IP           : {}", ip);
        log.info("Class Method : {}.{}", className, methodName);
        
        try {
            // 尝试打印请求参数，忽略过长或无法序列化的参数
            Object[] args = joinPoint.getArgs();
            if (args != null && args.length > 0) {
                 // 简单处理，只打印第一个参数作为 body 参考，实际生产可能需要更复杂的判断
                 // 防止打印 HttpServletRequest 等对象导致异常
                 try {
                     log.info("Request Args : {}", objectMapper.writeValueAsString(args));
                 } catch (Exception e) {
                     log.warn("Request Args : Unable to serialize args");
                 }
            }
        } catch (Exception e) {
            // 忽略参数打印异常，不影响主流程
        }

        Object result;
        try {
            result = joinPoint.proceed();
        } catch (Throwable e) {
            long timeCost = System.currentTimeMillis() - startTime;
            log.error("Exception    : {} ({}ms)", e.getMessage(), timeCost);
            log.info("========================================== Error ==========================================");
            throw e;
        }

        long timeCost = System.currentTimeMillis() - startTime;
        
        // 打印响应日志
        try {
            log.info("Response     : {}", objectMapper.writeValueAsString(result));
        } catch (Exception e) {
            log.warn("Response     : Unable to serialize result");
        }
        
        log.info("Time Cost    : {}ms", timeCost);
        log.info("=========================================== End ===========================================");
        
        return result;
    }
}
