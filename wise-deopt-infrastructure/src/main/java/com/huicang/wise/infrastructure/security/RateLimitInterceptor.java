package com.huicang.wise.infrastructure.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.lang.reflect.Method;

@Component
@Slf4j
public class RateLimitInterceptor implements HandlerInterceptor {

    @Autowired
    private RateLimiterService rateLimiterService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        HandlerMethod handlerMethod = (HandlerMethod) handler;
        Method method = handlerMethod.getMethod();
        RateLimit rateLimit = method.getAnnotation(RateLimit.class);

        if (rateLimit == null) {
            return true;
        }

        String ip = getClientIp(request);
        String uri = request.getRequestURI();
        Long userId = getUserId(request);

        boolean ipAllowed = rateLimiterService.allowByIp(ip, rateLimit.ipLimit(), rateLimit.ipWindowSeconds());
        if (!ipAllowed) {
            sendErrorResponse(response, 429, "IP请求频率过高");
            return false;
        }

        if (userId != null) {
            boolean userAllowed = rateLimiterService.allowByUser(userId, rateLimit.userLimit(), rateLimit.userWindowSeconds());
            if (!userAllowed) {
                sendErrorResponse(response, 429, "用户请求频率过高");
                return false;
            }
        }

        boolean apiAllowed = rateLimiterService.allowByApi(uri, rateLimit.apiLimit(), rateLimit.apiWindowSeconds());
        if (!apiAllowed) {
            sendErrorResponse(response, 429, "接口请求频率过高");
            return false;
        }

        return true;
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    private Long getUserId(HttpServletRequest request) {
        Object userId = request.getAttribute("userId");
        if (userId instanceof Long) {
            return (Long) userId;
        } else if (userId instanceof Integer) {
            return ((Integer) userId).longValue();
        }
        return null;
    }

    private void sendErrorResponse(HttpServletResponse response, int status, String message) throws Exception {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":" + status + ",\"message\":\"" + message + "\",\"data\":null}");
    }
}
