package com.huicang.wise.api.interceptor;

import com.huicang.wise.application.auth.AuthApplicationService;
import com.huicang.wise.common.annotation.RequiresPermission;
import com.huicang.wise.common.api.ErrorCode;
import com.huicang.wise.common.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.method.HandlerMethod;

import java.lang.reflect.Method;

/**
 * 类功能描述：认证拦截器，用于验证请求头中的Authorization令牌
 *
 * @author xingchentye
 * @version 1.0
 * @since 2026-02-07
 */
@Component
public class AuthenticationInterceptor implements HandlerInterceptor {

    private final AuthApplicationService authApplicationService;

    public AuthenticationInterceptor(AuthApplicationService authApplicationService) {
        this.authApplicationService = authApplicationService;
    }

    /**
     * 方法功能描述：请求预处理，验证Token
     *
     * @param request  请求对象
     * @param response 响应对象
     * @param handler  处理器
     * @return true-放行，false-拦截
     * @throws Exception 异常
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 如果不是映射到方法直接通过（例如静态资源）
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        // 获取Authorization头
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "缺少有效的身份认证信息");
        }

        String token = authHeader.substring(7);
        String username;
        try {
            // 验证Token，如果验证失败会抛出BusinessException
            username = authApplicationService.validateToken(token);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.AUTH_INVALID_TOKEN, "Token验证失败");
        }

        // 检查权限注解
        HandlerMethod handlerMethod = (HandlerMethod) handler;
        Method method = handlerMethod.getMethod();
        RequiresPermission requiresPermission = method.getAnnotation(RequiresPermission.class);
        
        // 如果方法上没有，检查类上是否有
        if (requiresPermission == null) {
            requiresPermission = handlerMethod.getBeanType().getAnnotation(RequiresPermission.class);
        }

        if (requiresPermission != null) {
            authApplicationService.checkPermission(username, requiresPermission.value());
        }

        return true;
    }
}
