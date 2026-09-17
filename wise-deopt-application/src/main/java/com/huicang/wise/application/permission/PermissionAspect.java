package com.huicang.wise.application.permission;

import com.huicang.wise.common.annotation.RequiresPermission;
import com.huicang.wise.common.api.ErrorCode;
import com.huicang.wise.common.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Aspect
@Component
public class PermissionAspect {

    private static final Logger log = LoggerFactory.getLogger(PermissionAspect.class);

    private final PermissionService permissionService;

    public PermissionAspect(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @Around("@annotation(requiresPermission)")
    public Object checkPermission(ProceedingJoinPoint joinPoint, RequiresPermission requiresPermission) throws Throwable {
        HttpServletRequest request = getCurrentRequest();
        if (request == null) {
            throw new RuntimeException("无法获取请求上下文");
        }

        Long userId = (Long) request.getAttribute("userId");
        
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户未登录");
        }

        String requiredPermission = requiresPermission.value();
        if (!permissionService.hasPermission(userId, requiredPermission)) {
            log.warn("用户权限不足: userId={}, requiredPermission={}", userId, requiredPermission);
            throw new BusinessException(ErrorCode.FORBIDDEN, "权限不足，需要权限: " + requiredPermission);
        }

        return joinPoint.proceed();
    }

    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }
}
