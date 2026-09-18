package com.huicang.wise.api.security;

import com.huicang.wise.application.accesskey.AccessKeyApplicationService;
import com.huicang.wise.application.accesskey.AccessKeyDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 访问密钥审计拦截器：解析请求中的访问密钥并在请求结束时记录审计日志。
 *
 * @author WiseDepot
 * @version 1.1
 * @since 2026-02-27
 */
@Component
public class AccessKeyAuditInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(AccessKeyAuditInterceptor.class);

    private final AccessKeyApplicationService accessKeyApplicationService;

    private static final String ACCESS_KEY_HEADER = "X-Access-Key";
    private static final String ACCESS_SECRET_HEADER = "X-Access-Secret";

    public AccessKeyAuditInterceptor(AccessKeyApplicationService accessKeyApplicationService) {
        this.accessKeyApplicationService = accessKeyApplicationService;
    }

    @Override
    public boolean preHandle(
            HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        String accessKey = request.getHeader(ACCESS_KEY_HEADER);
        String accessSecret = request.getHeader(ACCESS_SECRET_HEADER);

        if (accessKey != null && accessSecret != null) {
            // 经应用服务反查，入口层不直接访问仓储（STD-ARCH-02）
            AccessKeyDTO userAccessKey = accessKeyApplicationService.findByAccessKey(accessKey);
            if (userAccessKey != null) {
                request.setAttribute("accessKeyId", userAccessKey.getKeyId());
                request.setAttribute("accessKeyUserId", userAccessKey.getUserId());
                request.setAttribute("accessKeyValue", accessKey);
                request.setAttribute("requestStartTime", System.currentTimeMillis());
            }
        }
        return true;
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
            throws Exception {
        Long keyId = (Long) request.getAttribute("accessKeyId");
        Long userId = (Long) request.getAttribute("accessKeyUserId");
        String accessKey = (String) request.getAttribute("accessKeyValue");
        Long startTime = (Long) request.getAttribute("requestStartTime");

        if (keyId != null && userId != null && startTime != null) {
            long responseTimeMs = System.currentTimeMillis() - startTime;
            String requestUri = request.getRequestURI();
            String method = request.getMethod();
            String ipAddress = getClientIp(request);
            Short statusCode = (short) response.getStatus();
            String resultMessage = statusCode >= 200 && statusCode < 300 ? "Success" : "Failed";

            try {
                accessKeyApplicationService.updateLastUsed(keyId);
                accessKeyApplicationService.recordAccessLog(
                        userId,
                        accessKey,
                        requestUri,
                        method,
                        ipAddress,
                        statusCode,
                        resultMessage,
                        (int) responseTimeMs);
            } catch (Exception e) {
                logger.error("Failed to record access log: {}", e.getMessage(), e);
            }
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
