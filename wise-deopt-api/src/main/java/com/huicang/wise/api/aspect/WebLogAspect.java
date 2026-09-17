package com.huicang.wise.api.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.multipart.MultipartFile;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 类功能描述：Web请求日志切面
 *
 * @author xingchentye
 * @date 2026-01-23
 */
@Aspect
@Component
public class WebLogAspect {

    private static final Logger log = LoggerFactory.getLogger(WebLogAspect.class);

    /**
     * 使用 Spring 管理的 ObjectMapper（包含 JavaTimeModule 等配置），避免日志序列化时频繁出现
     * “Unable to serialize result” 的误报。
     */
    @Autowired
    private ObjectMapper objectMapper;

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

        // 排除心跳接口日志
        if ("receiveHeartbeat".equals(methodName) && "com.huicang.wise.api.controller.DeviceController".equals(className)) {
            return joinPoint.proceed();
        }

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
                 try {
                     List<Object> logArgs = Arrays.stream(args)
                         .map(arg -> {
                             if (arg instanceof MultipartFile) {
                                 MultipartFile file = (MultipartFile) arg;
                                 return "File: " + file.getOriginalFilename() + " (" + file.getSize() + " bytes)";
                             } else if (arg instanceof HttpServletRequest) {
                                 return "HttpServletRequest";
                             } else if (arg instanceof HttpServletResponse) {
                                 return "HttpServletResponse";
                             } else if (arg instanceof byte[]) {
                                 return "byte[" + ((byte[]) arg).length + "]";
                             }
                             return arg;
                         })
                         .collect(Collectors.toList());
                     
                     log.info("Request Args : {}", objectMapper.writeValueAsString(logArgs));
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
            if (result == null) {
                log.info("Response     : null (void or empty)");
            } else if (result instanceof byte[]) {
                log.info("Response     : byte[{}]", ((byte[]) result).length);
            } else if (result instanceof MultipartFile) {
                log.info("Response     : MultipartFile");
            } else {
                log.info("Response     : {}", objectMapper.writeValueAsString(result));
            }
        } catch (Exception e) {
            log.warn("Response     : Unable to serialize result");
        }
        
        log.info("Time Cost    : {}ms", timeCost);
        log.info("=========================================== End ===========================================");
        
        return result;
    }
}
