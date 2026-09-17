package com.huicang.wise.api.security;

import com.huicang.wise.application.permission.PermissionService;
import com.huicang.wise.domain.auth.port.TokenVerifier;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 权限拦截器：解析请求中的令牌并写入用户上下文属性。
 *
 * @author WiseDepot
 * @version 1.1
 * @since 2026-02-27
 */
@Component
public class PermissionInterceptor implements HandlerInterceptor {

    private final TokenVerifier tokenVerifier;
    private final PermissionService permissionService;

    public PermissionInterceptor(TokenVerifier tokenVerifier, PermissionService permissionService) {
        this.tokenVerifier = tokenVerifier;
        this.permissionService = permissionService;
    }

    /**
     * 从请求头解析 Bearer 令牌。
     *
     * <p>原实现调用基础设施层的 {@code resolveToken(HttpServletRequest)}，会使入口层依赖 infrastructure，
     * 并让领域端口暴露 Servlet 类型；此处就地解析，保持端口与 Web 技术无关（STD-ARCH-05）。
     *
     * @param request 当前请求
     * @return 令牌字符串；不存在时返回 null
     */
    private String resolveBearerToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String token = resolveBearerToken(request);
        if (token != null && tokenVerifier.validateToken(token)) {
            Long userId = tokenVerifier.getUserIdFromToken(token);
            if (userId != null) {
                request.setAttribute("userId", userId);
                request.setAttribute("permissions", permissionService.getUserPermissions(userId));
            }
        }
        return true;
    }
}
