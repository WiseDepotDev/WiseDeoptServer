package com.huicang.wise.api.security;

import com.huicang.wise.application.permission.PermissionService;
import com.huicang.wise.infrastructure.security.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class PermissionInterceptor implements HandlerInterceptor {

    private final JwtTokenProvider jwtTokenProvider;
    private final PermissionService permissionService;

    public PermissionInterceptor(JwtTokenProvider jwtTokenProvider, PermissionService permissionService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.permissionService = permissionService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String token = jwtTokenProvider.resolveToken(request);
        if (token != null && jwtTokenProvider.validateToken(token)) {
            Long userId = jwtTokenProvider.getUserIdFromToken(token);
            if (userId != null) {
                request.setAttribute("userId", userId);
                request.setAttribute("permissions", permissionService.getUserPermissions(userId));
            }
        }
        return true;
    }
}
