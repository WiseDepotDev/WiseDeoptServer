package com.huicang.wise.infrastructure.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        long startTime = System.currentTimeMillis();

        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        try {
            filterChain.doFilter(wrappedRequest, wrappedResponse);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            logRequest(wrappedRequest, wrappedResponse, duration);
            wrappedResponse.copyBodyToResponse();
        }
    }

    private void logRequest(ContentCachingRequestWrapper request, ContentCachingResponseWrapper response, long duration) {
        StringBuilder logMessage = new StringBuilder();
        logMessage.append("\n==================== 请求信息 ====================\n");
        logMessage.append(String.format("请求方法: %s\n", request.getMethod()));
        logMessage.append(String.format("请求URI: %s\n", request.getRequestURI()));
        logMessage.append(String.format("查询字符串: %s\n", request.getQueryString()));
        logMessage.append(String.format("远程地址: %s\n", request.getRemoteAddr()));
        logMessage.append(String.format("请求耗时: %d ms\n", duration));

        logMessage.append("请求头:\n");
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            String headerValue = request.getHeader(headerName);
            logMessage.append(String.format("  %s: %s\n", headerName, headerValue));
        }

        logMessage.append("请求参数:\n");
        Map<String, String[]> parameterMap = request.getParameterMap();
        if (!parameterMap.isEmpty()) {
            parameterMap.forEach((key, values) -> {
                logMessage.append(String.format("  %s: %s\n", key, String.join(", ", values)));
            });
        } else {
            logMessage.append("  无\n");
        }

        logMessage.append("请求体:\n");
        byte[] content = request.getContentAsByteArray();
        if (content.length > 0) {
            String contentType = request.getContentType();
            if (contentType != null && (contentType.startsWith("image/") || contentType.startsWith("multipart/form-data"))) {
                logMessage.append(String.format("  [二进制内容: %s, %d bytes]\n", contentType, content.length));
            } else {
                String requestBody = new String(content, StandardCharsets.UTF_8);
                // 限制请求体长度，避免过长
                if (requestBody.length() > 2000) {
                    requestBody = requestBody.substring(0, 2000) + "... (截断)";
                }
                logMessage.append(String.format("  %s\n", requestBody));
            }
        } else {
            logMessage.append("  无\n");
        }

        logMessage.append(String.format("响应状态: %d\n", response.getStatus()));
        logMessage.append("响应头:\n");
        for (String headerName : response.getHeaderNames()) {
            String headerValue = response.getHeader(headerName);
            logMessage.append(String.format("  %s: %s\n", headerName, headerValue));
        }

        logMessage.append("响应体:\n");
        byte[] responseContent = response.getContentAsByteArray();
        if (responseContent.length > 0) {
            String contentType = response.getContentType();
            if (contentType != null && (contentType.startsWith("image/") || contentType.startsWith("application/octet-stream"))) {
                logMessage.append(String.format("  [二进制内容: %s, %d bytes]\n", contentType, responseContent.length));
            } else {
                String responseBody = new String(responseContent, StandardCharsets.UTF_8);
                // 限制响应体长度
                if (responseBody.length() > 2000) {
                    responseBody = responseBody.substring(0, 2000) + "... (截断)";
                }
                logMessage.append(String.format("  %s\n", responseBody));
            }
        } else {
            logMessage.append("  无\n");
        }

        logMessage.append("==================== 请求结束 ====================\n");

        log.info(logMessage.toString());
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.contains("/actuator") || 
               path.contains("/swagger") || 
               path.contains("/api-docs") ||
               path.contains("/webjars") ||
               path.contains("/favicon.ico") ||
               path.equals("/api/device/heartbeat"); // 排除心跳日志
    }
}
